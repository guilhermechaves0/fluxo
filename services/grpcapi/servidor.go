// Package grpcapi é o adaptador gRPC do importador: traduz entre o contrato gerado de protos/ e o
// domínio de importador/dominio. Só este pacote conhece gRPC; o domínio não importa nada daqui.
package grpcapi

import (
	"context"
	"errors"
	"fmt"
	"log/slog"

	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	importadorv1 "github.com/guilhermechaves0/fluxo/services/gen/fluxo/importador/v1"
	"github.com/guilhermechaves0/fluxo/services/importador/dominio"
	"github.com/guilhermechaves0/fluxo/services/importador/pdftexto"
)

// TamanhoMaximo é o maior extrato aceito: um extrato de vários anos ainda cabe com folga.
const TamanhoMaximo = 5 << 20 // 5 MB

// Leitor lê o conteúdo de um arquivo de extrato. Fica do lado de quem usa, para os testes trocarem a leitura.
type Leitor func(ctx context.Context, conteudo []byte) (dominio.Extrato, error)

// Servidor implementa o ImportadorService. Embute o Unimplemented para continuar compilando quando o
// contrato ganhar um rpc novo.
type Servidor struct {
	importadorv1.UnimplementedImportadorServiceServer
	ler Leitor
}

// NovoServidor devolve o servidor com a leitura de produção (OFX, CSV e PDF).
func NovoServidor() *Servidor {
	return &Servidor{ler: LerArquivo}
}

// NovoServidorCom devolve o servidor com outra leitura, para os testes.
func NovoServidorCom(ler Leitor) *Servidor {
	return &Servidor{ler: ler}
}

// LerArquivo manda o PDF para a extração de texto antes do domínio; OFX e CSV vão direto.
func LerArquivo(ctx context.Context, conteudo []byte) (dominio.Extrato, error) {
	if !pdftexto.EhPDF(conteudo) {
		return dominio.LerExtrato(ctx, conteudo)
	}
	linhas, err := pdftexto.Linhas(ctx, conteudo)
	if err != nil {
		if ctx.Err() != nil {
			return dominio.Extrato{}, err
		}
		return dominio.Extrato{}, fmt.Errorf("%w: %v", dominio.ErrFormatoDesconhecido, err)
	}
	return dominio.LerTextoDeFatura(ctx, linhas)
}

// Importar lê o extrato e devolve os lançamentos. O prazo de quem chama chega no ctx e vai até o laço de
// leitura do domínio. O conteúdo do extrato não vai para o log, só as contagens.
func (s *Servidor) Importar(ctx context.Context, req *importadorv1.ImportarRequest) (*importadorv1.ImportarResponse, error) {
	if len(req.GetConteudo()) > TamanhoMaximo {
		return nil, status.Error(codes.ResourceExhausted, "O extrato passa de 5 MB.")
	}
	extrato, err := s.ler(ctx, req.GetConteudo())
	if err != nil {
		return nil, paraStatus(err)
	}
	slog.Info("extrato lido", "formato", extrato.Formato, "origem", extrato.Origem,
		"lancamentos", len(extrato.Lancamentos), "ignorados", len(extrato.Ignorados))
	return paraResposta(extrato), nil
}

// paraStatus traduz o erro do domínio no código gRPC que a api sabe interpretar.
func paraStatus(err error) error {
	switch {
	case errors.Is(err, context.DeadlineExceeded):
		return status.Error(codes.DeadlineExceeded, err.Error())
	case errors.Is(err, context.Canceled):
		return status.Error(codes.Canceled, err.Error())
	case errors.Is(err, dominio.ErrArquivoVazio), errors.Is(err, dominio.ErrFormatoDesconhecido):
		return status.Error(codes.InvalidArgument, err.Error())
	default:
		slog.Error("erro inesperado ao ler o extrato", "erro", err)
		return status.Error(codes.Internal, "erro interno ao ler o extrato")
	}
}

func paraResposta(e dominio.Extrato) *importadorv1.ImportarResponse {
	r := &importadorv1.ImportarResponse{Origem: e.Origem, Formato: formato(e.Formato)}
	for _, l := range e.Lancamentos {
		r.Lancamentos = append(r.Lancamentos, &importadorv1.Lancamento{IdExterno: l.IDExterno, Data: l.Data,
			ValorCentavos: l.ValorCentavos, Descricao: l.Descricao, Tipo: tipo(l.Tipo), Categoria: l.Categoria})
	}
	for _, i := range e.Ignorados {
		r.LinhasIgnoradas = append(r.LinhasIgnoradas, &importadorv1.LinhaIgnorada{Posicao: int32(i.Posicao),
			Motivo: i.Motivo})
	}
	r.Ignorados = int32(len(e.Ignorados))
	return r
}

func formato(f dominio.Formato) importadorv1.Formato {
	switch f {
	case dominio.FormatoOFX:
		return importadorv1.Formato_FORMATO_OFX
	case dominio.FormatoCSV:
		return importadorv1.Formato_FORMATO_CSV
	case dominio.FormatoPDF:
		return importadorv1.Formato_FORMATO_PDF
	}
	return importadorv1.Formato_FORMATO_NAO_ESPECIFICADO
}

func tipo(t dominio.Tipo) importadorv1.Tipo {
	switch t {
	case dominio.Receita:
		return importadorv1.Tipo_TIPO_RECEITA
	case dominio.Despesa:
		return importadorv1.Tipo_TIPO_DESPESA
	}
	return importadorv1.Tipo_TIPO_NAO_ESPECIFICADO
}
