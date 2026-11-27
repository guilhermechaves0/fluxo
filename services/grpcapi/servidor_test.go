package grpcapi

import (
	"bytes"
	"context"
	"errors"
	"net"
	"os"
	"testing"
	"time"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"

	importadorv1 "github.com/guilhermechaves0/fluxo/services/gen/fluxo/importador/v1"
	"github.com/guilhermechaves0/fluxo/services/importador/dominio"
)

const tamanhoDoBuffer = 1 << 20

// cliente sobe o servidor numa conexão em memória (bufconn): o teste passa pela pilha gRPC inteira,
// com serialização, prazo e códigos de status, sem abrir porta de rede.
func cliente(t *testing.T, servidor *Servidor) importadorv1.ImportadorServiceClient {
	t.Helper()
	lis := bufconn.Listen(tamanhoDoBuffer)
	srv := grpc.NewServer(grpc.MaxRecvMsgSize(TamanhoMaximo + tamanhoDoBuffer))
	importadorv1.RegisterImportadorServiceServer(srv, servidor)
	go func() { _ = srv.Serve(lis) }()
	t.Cleanup(srv.Stop)

	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(context.Context, string) (net.Conn, error) { return lis.Dial() }),
		grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = conn.Close() })
	return importadorv1.NewImportadorServiceClient(conn)
}

func arquivo(t *testing.T, nome string) []byte {
	t.Helper()
	conteudo, err := os.ReadFile("../importador/dominio/testdata/" + nome)
	if err != nil {
		t.Fatal(err)
	}
	return conteudo
}

func TestImportarDevolveOsLancamentos(t *testing.T) {
	casos := []struct {
		nome        string
		arquivo     string
		formato     importadorv1.Formato
		origem      string
		lancamentos int
		ignoradas   int
	}{
		{"OFX do Nubank", "nubank-conta.ofx", importadorv1.Formato_FORMATO_OFX, "Nubank", 3, 1},
		{"fatura em PDF do Banco do Brasil", "bb-fatura.pdf", importadorv1.Formato_FORMATO_PDF,
			"Banco do Brasil (fatura do cartão)", 8, 0},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			resp, err := cliente(t, NovoServidor()).Importar(context.Background(),
				&importadorv1.ImportarRequest{Conteudo: arquivo(t, c.arquivo)})
			if err != nil {
				t.Fatalf("erro inesperado: %v", err)
			}
			if resp.GetFormato() != c.formato || resp.GetOrigem() != c.origem ||
				len(resp.GetLancamentos()) != c.lancamentos || len(resp.GetLinhasIgnoradas()) != c.ignoradas ||
				int(resp.GetIgnorados()) != c.ignoradas {
				t.Fatalf("resposta inesperada: %v", resp)
			}
		})
	}
}

func TestImportarTraduzOsCamposDoLancamento(t *testing.T) {
	resp, err := cliente(t, NovoServidor()).Importar(context.Background(),
		&importadorv1.ImportarRequest{Conteudo: arquivo(t, "nubank-conta.ofx")})
	if err != nil {
		t.Fatal(err)
	}
	l := resp.GetLancamentos()[1]
	if l.GetTipo() != importadorv1.Tipo_TIPO_DESPESA || l.GetValorCentavos() != 4590 {
		t.Errorf("segundo lançamento errado: %v", l)
	}
	fatura, err := cliente(t, NovoServidor()).Importar(context.Background(),
		&importadorv1.ImportarRequest{Conteudo: arquivo(t, "bb-fatura.pdf")})
	if err != nil {
		t.Fatal(err)
	}
	if fatura.GetLancamentos()[1].GetCategoria() != "Restaurantes" {
		t.Errorf("a seção da fatura deveria virar categoria: %v", fatura.GetLancamentos()[1])
	}
}

func TestErrosDoDominioViramCodigosGRPC(t *testing.T) {
	casos := []struct {
		nome     string
		conteudo []byte
		ler      Leitor
		codigo   codes.Code
	}{
		{"arquivo vazio", []byte("   "), nil, codes.InvalidArgument},
		{"formato desconhecido", []byte("coluna1;coluna2\nx;y\n"), nil, codes.InvalidArgument},
		{"PDF que não é fatura", arquivo(t, "bb-fatura.pdf")[:200], nil, codes.InvalidArgument},
		{"grande demais", bytes.Repeat([]byte("x"), TamanhoMaximo+1), nil, codes.ResourceExhausted},
		{"prazo vencido no domínio", []byte("x"), func(context.Context, []byte) (dominio.Extrato, error) {
			return dominio.Extrato{}, errors.Join(errors.New("leitura interrompida"), context.DeadlineExceeded)
		}, codes.DeadlineExceeded},
		{"cancelado no domínio", []byte("x"), func(context.Context, []byte) (dominio.Extrato, error) {
			return dominio.Extrato{}, context.Canceled
		}, codes.Canceled},
		{"erro inesperado", []byte("x"), func(context.Context, []byte) (dominio.Extrato, error) {
			return dominio.Extrato{}, errors.New("falha com dado sigiloso")
		}, codes.Internal},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			servidor := NovoServidor()
			if c.ler != nil {
				servidor = NovoServidorCom(c.ler)
			}
			_, err := cliente(t, servidor).Importar(context.Background(), &importadorv1.ImportarRequest{Conteudo: c.conteudo})
			if status.Code(err) != c.codigo {
				t.Fatalf("esperava %v, obteve %v", c.codigo, err)
			}
			if c.codigo == codes.Internal && bytes.Contains([]byte(err.Error()), []byte("sigiloso")) {
				t.Errorf("o erro interno não pode vazar para quem chama: %v", err)
			}
		})
	}
}

func TestPrazoDeQuemChamaChegaAoDominio(t *testing.T) {
	// A leitura espera o contexto: se o prazo do cliente chegasse ao servidor, ela termina com o erro do
	// contexto dentro do prazo e vira DEADLINE_EXCEEDED.
	lenta := func(ctx context.Context, _ []byte) (dominio.Extrato, error) {
		<-ctx.Done()
		return dominio.Extrato{}, ctx.Err()
	}
	ctx, cancelar := context.WithTimeout(context.Background(), 100*time.Millisecond)
	defer cancelar()
	inicio := time.Now()
	_, err := cliente(t, NovoServidorCom(lenta)).Importar(ctx, &importadorv1.ImportarRequest{Conteudo: []byte("x")})
	if status.Code(err) != codes.DeadlineExceeded {
		t.Fatalf("esperava DEADLINE_EXCEEDED, obteve %v", err)
	}
	if gasto := time.Since(inicio); gasto > 2*time.Second {
		t.Errorf("a chamada deveria terminar perto do prazo, levou %v", gasto)
	}
}
