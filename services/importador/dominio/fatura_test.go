package dominio

import (
	"errors"
	"strings"
	"testing"
)

// Linhas como a extração de texto entrega a fatura do Ourocard, com dados inventados.
var faturaDeExemplo = []string{
	"OUROCARD INTERNACIONAL. Final 0000",
	"Fatura fechada em 29/08/2026",
	"Data Descrição País Valor",
	"SALDO FATURA ANTERIOR BR R$ 1.200,00",
	"Pagamentos/Créditos",
	"05/08 PGTO. DEBITO CONTA 0000 BR R$ -1.200,00",
	"Restaurantes",
	"12/08 PADARIA EXEMPLO NATAL BR R$ 45,90",
	"Serviços",
	"20/08 LOJA ONLINE EXEMPLO US R$ 110,00",
	"*** 20,00 USD",
	"Cotação do Dólar de 20/08: R$ 5,5000",
	"20/08 IOF - COMPRA NO EXTERIOR R$ 3,85",
	"Compras parceladas",
	"30/12 LOJA ANTIGA PARC 09/10 NATAL BR R$ 80,00",
	"12/08 PADARIA EXEMPLO NATAL BR R$ 45,90",
	"31/02 DATA QUE NAO EXISTE BR R$ 9,99",
	"Total da Fatura R$ 485,65",
	"05/09 DEPOIS DO TOTAL BR R$ 1,00",
}

func TestLerTextoDeFatura(t *testing.T) {
	extrato, err := LerTextoDeFatura(faturaDeExemplo)
	if err != nil {
		t.Fatal(err)
	}
	if extrato.Formato != FormatoPDF || extrato.Origem != "Banco do Brasil (fatura do cartão)" {
		t.Errorf("formato ou origem errados: %s, %s", extrato.Formato, extrato.Origem)
	}
	queridos := []Lancamento{
		{Data: "2026-08-05", ValorCentavos: 120000, Descricao: "PGTO. DEBITO CONTA 0000", Tipo: Receita,
			Categoria: "Pagamentos/Créditos"},
		{Data: "2026-08-12", ValorCentavos: 4590, Descricao: "PADARIA EXEMPLO NATAL", Tipo: Despesa, Categoria: "Restaurantes"},
		{Data: "2026-08-20", ValorCentavos: 11000, Descricao: "LOJA ONLINE EXEMPLO", Tipo: Despesa, Categoria: "Serviços"},
		{Data: "2026-08-20", ValorCentavos: 385, Descricao: "IOF - COMPRA NO EXTERIOR", Tipo: Despesa, Categoria: "Serviços"},
		{Data: "2025-12-30", ValorCentavos: 8000, Descricao: "LOJA ANTIGA PARC 09/10 NATAL", Tipo: Despesa,
			Categoria: "Compras parceladas"},
		{Data: "2026-08-12", ValorCentavos: 4590, Descricao: "PADARIA EXEMPLO NATAL", Tipo: Despesa,
			Categoria: "Compras parceladas"},
	}
	if len(extrato.Lancamentos) != len(queridos) {
		t.Fatalf("esperava %d lançamentos, obteve %+v", len(queridos), extrato.Lancamentos)
	}
	for i, q := range queridos {
		obtido := extrato.Lancamentos[i]
		q.IDExterno = obtido.IDExterno
		if obtido != q {
			t.Errorf("lançamento %d:\nesperava %+v\nobteve   %+v", i, q, obtido)
		}
	}
	if extrato.Lancamentos[1].IDExterno == extrato.Lancamentos[5].IDExterno {
		t.Error("duas compras iguais no mesmo dia precisam de identificadores diferentes")
	}
	ignorados := extrato.Ignorados
	if len(ignorados) != 1 || ignorados[0].Posicao != 17 || !strings.HasPrefix(ignorados[0].Motivo, "data inválida") {
		t.Errorf("esperava só a data 31/02 ignorada, na linha 17: %+v", ignorados)
	}
}

func TestLerTextoDeFaturaRecusaOQueNaoEhFaturaDoBB(t *testing.T) {
	casos := map[string][]string{
		"outro PDF":        {"Boleto de condomínio", "Vencimento 10/09/2026"},
		"sem tabela":       {"OUROCARD", "Fatura fechada em 29/08/2026", "Resumo da fatura"},
		"sem data nenhuma": {"OUROCARD", "Data Descrição País Valor", "12/08 PADARIA BR R$ 1,00"},
	}
	for nome, linhas := range casos {
		t.Run(nome, func(t *testing.T) {
			if _, err := LerTextoDeFatura(linhas); !errors.Is(err, ErrFormatoDesconhecido) {
				t.Fatalf("esperava ErrFormatoDesconhecido, obteve %v", err)
			}
		})
	}
}

func TestTituloDeSecaoDeixaDeForaSaldoCotacaoETitular(t *testing.T) {
	casos := map[string]bool{
		"Restaurantes":                         true,
		"Compras parceladas":                   true,
		"Pagamentos/Créditos":                  true,
		"SALDO FATURA ANTERIOR BR R$ 1.200,00": false,
		"*** 20,00 USD":                        false,
		"Cotação do Dólar de 20/08: R$ 5,5000": false,
		"FULANO DE TAL (Cartão 1234)":          false,
		"":                                     false,
	}
	for linha, esperado := range casos {
		if tituloDeSecao(linha) != esperado {
			t.Errorf("tituloDeSecao(%q) deveria ser %v", linha, esperado)
		}
	}
}
