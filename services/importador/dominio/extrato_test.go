package dominio

import (
	"errors"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

// Os arquivos de testdata são sintéticos: seguem o formato de exportação de cada banco com
// dados inventados.
func TestLerExtrato(t *testing.T) {
	casos := []struct {
		arquivo     string
		formato     Formato
		origem      string
		lancamentos []Lancamento
		ignorados   int
	}{
		{
			arquivo: "nubank-conta.ofx", formato: FormatoOFX, origem: "Nubank", ignorados: 1,
			lancamentos: []Lancamento{
				{IDExterno: "a1b2c3d4-0001", Data: "2026-09-05", ValorCentavos: 450000,
					Descricao: "Transferência recebida pelo Pix - EMPRESA EXEMPLO LTDA", Tipo: Receita},
				{IDExterno: "a1b2c3d4-0002", Data: "2026-09-06", ValorCentavos: 4590,
					Descricao: "Compra no débito - PADARIA EXEMPLO", Tipo: Despesa},
				{IDExterno: "a1b2c3d4-0003", Data: "2026-09-08", ValorCentavos: 150000,
					Descricao: "Pagamento de fatura", Tipo: Despesa},
			},
		},
		{
			arquivo: "bb.ofx", formato: FormatoOFX, origem: "Banco do Brasil", ignorados: 1,
			lancamentos: []Lancamento{
				{IDExterno: "20260910001", Data: "2026-09-10", ValorCentavos: 8935,
					Descricao: "Pagamento de Boleto - ÁGUA E ESGOTO", Tipo: Despesa},
				{IDExterno: "20260912001", Data: "2026-09-12", ValorCentavos: 25000,
					Descricao: "Pix - Recebido - JOÃO EXEMPLO", Tipo: Receita},
			},
		},
		{
			arquivo: "c6-conta.ofx", formato: FormatoOFX, origem: "C6 Bank",
			lancamentos: []Lancamento{
				{IDExterno: "c6-0001", Data: "2026-09-14", ValorCentavos: 2380, Descricao: "Pix enviado - CORRIDA & CIA",
					Tipo: Despesa},
				{IDExterno: "c6-0002", Data: "2026-09-15", ValorCentavos: 80000, Descricao: "TED recebida - CLIENTE EXEMPLO",
					Tipo: Receita},
			},
		},
		{
			arquivo: "nubank-cartao.csv", formato: FormatoCSV, origem: "Nubank (fatura do cartão)",
			lancamentos: []Lancamento{
				{Data: "2026-09-03", ValorCentavos: 15237, Descricao: "Mercado Exemplo", Tipo: Despesa},
				{Data: "2026-09-04", ValorCentavos: 4890, Descricao: "Restaurante Exemplo", Tipo: Despesa},
				{Data: "2026-09-10", ValorCentavos: 150000, Descricao: "Pagamento recebido", Tipo: Receita},
				{Data: "2026-09-11", ValorCentavos: 2000, Descricao: "Estorno de \"Loja, Exemplo\"", Tipo: Receita},
			},
		},
		{
			arquivo: "nubank-conta.csv", formato: FormatoCSV, origem: "Nubank (conta)", ignorados: 1,
			lancamentos: []Lancamento{
				{IDExterno: "6a1f0000-0000-4000-8000-000000000001", Data: "2026-09-05", ValorCentavos: 450000,
					Descricao: "Transferência recebida pelo Pix - EMPRESA EXEMPLO", Tipo: Receita},
				{IDExterno: "6a1f0000-0000-4000-8000-000000000002", Data: "2026-09-06", ValorCentavos: 4590,
					Descricao: "Compra no débito - PADARIA EXEMPLO", Tipo: Despesa},
			},
		},
		{
			arquivo: "c6-fatura.csv", formato: FormatoCSV, origem: "C6 Bank (fatura do cartão)",
			lancamentos: []Lancamento{
				{Data: "2026-09-02", ValorCentavos: 15237, Descricao: "MERCADO EXEMPLO", Tipo: Despesa,
					Categoria: "Supermercados"},
				{Data: "2026-09-05", ValorCentavos: 9990, Descricao: "LOJA EXEMPLO (2/3)", Tipo: Despesa,
					Categoria: "Departamento"},
				{Data: "2026-09-06", ValorCentavos: 2000, Descricao: "ESTORNO LOJA EXEMPLO", Tipo: Receita,
					Categoria: "Departamento"},
			},
		},
		{
			arquivo: "c6-conta.csv", formato: FormatoCSV, origem: "C6 Bank (conta)", ignorados: 1,
			lancamentos: []Lancamento{
				{Data: "2026-09-14", ValorCentavos: 2380, Descricao: "Pix enviado - CORRIDA EXEMPLO", Tipo: Despesa},
				{Data: "2026-09-15", ValorCentavos: 80000, Descricao: "TED recebida - CLIENTE EXEMPLO", Tipo: Receita},
			},
		},
		{
			arquivo: "bb.csv", formato: FormatoCSV, origem: "Banco do Brasil", ignorados: 2,
			lancamentos: []Lancamento{
				{Data: "2026-09-10", ValorCentavos: 8935, Descricao: "Pagamento de Boleto - ÁGUA E ESGOTO",
					Tipo: Despesa},
				{Data: "2026-09-12", ValorCentavos: 25000, Descricao: "Pix - Recebido - 12/09 10:31 JOÃO EXEMPLO",
					Tipo: Receita},
			},
		},
	}
	for _, c := range casos {
		t.Run(c.arquivo, func(t *testing.T) {
			extrato, err := LerExtrato(ler(t, c.arquivo))
			if err != nil {
				t.Fatal(err)
			}
			if extrato.Formato != c.formato || extrato.Origem != c.origem {
				t.Errorf("esperava %s de %q, obteve %s de %q", c.formato, c.origem, extrato.Formato, extrato.Origem)
			}
			if len(extrato.Ignorados) != c.ignorados {
				t.Errorf("esperava %d ignorados, obteve %+v", c.ignorados, extrato.Ignorados)
			}
			if len(extrato.Lancamentos) != len(c.lancamentos) {
				t.Fatalf("esperava %d lançamentos, obteve %+v", len(c.lancamentos), extrato.Lancamentos)
			}
			for i, querido := range c.lancamentos {
				obtido := extrato.Lancamentos[i]
				if obtido.IDExterno == "" {
					t.Errorf("lançamento %d sem identificador", i)
				}
				if querido.IDExterno == "" {
					querido.IDExterno = obtido.IDExterno
				}
				if obtido != querido {
					t.Errorf("lançamento %d:\nesperava %+v\nobteve   %+v", i, querido, obtido)
				}
			}
		})
	}
}

func TestLerExtratoDuasVezesGeraOsMesmosIdentificadores(t *testing.T) {
	primeiro, _ := LerExtrato(ler(t, "c6-fatura.csv"))
	segundo, _ := LerExtrato(ler(t, "c6-fatura.csv"))
	for i := range primeiro.Lancamentos {
		if primeiro.Lancamentos[i].IDExterno != segundo.Lancamentos[i].IDExterno {
			t.Fatalf("o identificador mudou entre duas leituras do mesmo arquivo: %s e %s",
				primeiro.Lancamentos[i].IDExterno, segundo.Lancamentos[i].IDExterno)
		}
	}
}

func TestLerExtratoLinhasIguaisGanhamIdentificadoresDiferentes(t *testing.T) {
	csv := "date,title,amount\n2026-09-03,Café,8.50\n2026-09-03,Café,8.50\n"
	extrato, err := LerExtrato([]byte(csv))
	if err != nil {
		t.Fatal(err)
	}
	if len(extrato.Lancamentos) != 2 || extrato.Lancamentos[0].IDExterno == extrato.Lancamentos[1].IDExterno {
		t.Fatalf("dois cafés iguais no mesmo dia precisam virar dois lançamentos: %+v", extrato.Lancamentos)
	}
}

func TestLerExtratoRecusaArquivoVazioOuDesconhecido(t *testing.T) {
	if _, err := LerExtrato([]byte("  \n")); !errors.Is(err, ErrArquivoVazio) {
		t.Errorf("esperava ErrArquivoVazio, obteve %v", err)
	}
	_, err := LerExtrato(ler(t, "desconhecido.csv"))
	if !errors.Is(err, ErrFormatoDesconhecido) {
		t.Errorf("esperava ErrFormatoDesconhecido, obteve %v", err)
	}
}

func TestLerExtratoExplicaQuandoChegaPDFOuImagem(t *testing.T) {
	casos := map[string][]byte{
		"PDF":      []byte("%PDF-1.7\n%âãÏÓ\n1 0 obj"),
		"imagem":   {0x89, 'P', 'N', 'G', 0x0d, 0x0a},
		"planilha": []byte("PK\x03\x04xl/workbook.xml"),
	}
	for nome, conteudo := range casos {
		t.Run(nome, func(t *testing.T) {
			_, err := LerExtrato(conteudo)
			if !errors.Is(err, ErrFormatoDesconhecido) || !strings.Contains(err.Error(), "OFX ou CSV") {
				t.Fatalf("esperava explicação pedindo OFX ou CSV, obteve %v", err)
			}
		})
	}
}

func TestDataQueNaoExisteNoCalendarioEhRecusada(t *testing.T) {
	for _, data := range []string{"31/02/2026", "29/02/2026", "2026-04-31"} {
		if _, err := dataCSV(data); !errors.Is(err, ErrDataInvalida) {
			t.Errorf("%s deveria ser recusada, obteve %v", data, err)
		}
	}
	if d, err := dataCSV("29/02/2028"); err != nil || d != "2028-02-29" {
		t.Errorf("29/02/2028 existe (ano bissexto): %s, %v", d, err)
	}
}

func TestLerExtratoAceitaOFormatoAntigoSemCabecalho(t *testing.T) {
	extrato, err := LerExtrato([]byte("2026-09-06;Supermercado;-312,45\n05/09/2026;Salário;4500.00\n"))
	if err != nil {
		t.Fatal(err)
	}
	if len(extrato.Lancamentos) != 2 || extrato.Lancamentos[1].Tipo != Receita {
		t.Fatalf("esperava duas linhas lidas, obteve %+v", extrato)
	}
}

func ler(t *testing.T, arquivo string) []byte {
	t.Helper()
	conteudo, err := os.ReadFile(filepath.Join("testdata", arquivo))
	if err != nil {
		t.Fatal(err)
	}
	return conteudo
}
