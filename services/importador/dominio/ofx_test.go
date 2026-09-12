package dominio

import (
	"errors"
	"testing"
)

func TestParseSTMTTRN(t *testing.T) {
	casos := []struct {
		nome    string
		bloco   string
		querido Lancamento
		erro    error
	}{
		{
			nome: "despesa com memo",
			bloco: "<STMTTRN>\n<TRNTYPE>DEBIT\n<DTPOSTED>20260905120000[-3:BRT]\n" +
				"<TRNAMT>-45.90\n<FITID>2026090501\n<MEMO>PADARIA DO BAIRRO\n</STMTTRN>",
			querido: Lancamento{IDExterno: "2026090501", Data: "2026-09-05", ValorCentavos: 4590,
				Descricao: "PADARIA DO BAIRRO", Tipo: Despesa},
		},
		{
			nome:    "receita",
			bloco:   "<STMTTRN>\n<DTPOSTED>20260905\n<TRNAMT>4500.00\n<FITID>x\n<MEMO>SALARIO\n</STMTTRN>",
			querido: Lancamento{IDExterno: "x", Data: "2026-09-05", ValorCentavos: 450000, Descricao: "SALARIO", Tipo: Receita},
		},
		{nome: "sem valor", bloco: "<STMTTRN>\n<DTPOSTED>20260905\n</STMTTRN>", erro: ErrCampoAusente},
		{nome: "data curta", bloco: "<STMTTRN>\n<DTPOSTED>2026\n<TRNAMT>1\n</STMTTRN>", erro: ErrDataInvalida},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			obtido, err := ParseSTMTTRN(c.bloco)
			conferir(t, obtido, err, c.querido, c.erro)
		})
	}
}

func TestParseLinhaCSV(t *testing.T) {
	casos := []struct {
		nome    string
		linha   string
		querido Lancamento
		erro    error
	}{
		{nome: "despesa com vírgula", linha: "2026-09-06;Supermercado;-312,45",
			querido: Lancamento{Data: "2026-09-06", ValorCentavos: 31245, Descricao: "Supermercado", Tipo: Despesa}},
		{nome: "receita com ponto", linha: "2026-09-05;Salário;4500.00",
			querido: Lancamento{Data: "2026-09-05", ValorCentavos: 450000, Descricao: "Salário", Tipo: Receita}},
		{nome: "colunas faltando", linha: "2026-09-05;so-duas", erro: ErrCampoAusente},
		{nome: "data em outro formato", linha: "05/09/2026;x;1", erro: ErrDataInvalida},
		{nome: "valor inválido", linha: "2026-09-05;x;abc", erro: ErrValorInvalido},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			obtido, err := ParseLinhaCSV(c.linha)
			conferir(t, obtido, err, c.querido, c.erro)
		})
	}
}

func TestCentavos(t *testing.T) {
	casos := []struct {
		entrada string
		querido int64
		erro    error
	}{
		{"-45.90", -4590, nil},
		{"1.234,56", 123456, nil},
		{"12", 1200, nil},
		{"0,5", 50, nil},
		{"abc", 0, ErrValorInvalido},
		{"1.234", 0, ErrValorInvalido},
		{"", 0, ErrValorInvalido},
	}
	for _, c := range casos {
		t.Run(c.entrada, func(t *testing.T) {
			obtido, err := Centavos(c.entrada)
			if c.erro != nil {
				if !errors.Is(err, c.erro) {
					t.Fatalf("esperava erro %v, obteve %v", c.erro, err)
				}
				return
			}
			if err != nil || obtido != c.querido {
				t.Errorf("esperava %d, obteve %d (erro %v)", c.querido, obtido, err)
			}
		})
	}
}

func conferir(t *testing.T, obtido Lancamento, err error, querido Lancamento, erroQuerido error) {
	t.Helper()
	if erroQuerido != nil {
		if !errors.Is(err, erroQuerido) {
			t.Fatalf("esperava erro %v, obteve %v", erroQuerido, err)
		}
		return
	}
	if err != nil {
		t.Fatal(err)
	}
	if obtido != querido {
		t.Errorf("esperava %+v, obteve %+v", querido, obtido)
	}
}
