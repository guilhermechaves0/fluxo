package pdftexto

import (
	"errors"
	"os"
	"slices"
	"testing"
)

// bb-fatura.pdf é sintético: duas páginas no formato da fatura do Ourocard, com dados inventados.
func TestLinhasLeAsDuasPaginasNaOrdemEComAcentos(t *testing.T) {
	conteudo, err := os.ReadFile("../dominio/testdata/bb-fatura.pdf")
	if err != nil {
		t.Fatal(err)
	}
	if !EhPDF(conteudo) {
		t.Fatal("o arquivo de teste deveria ser reconhecido como PDF")
	}
	linhas, err := Linhas(conteudo)
	if err != nil {
		t.Fatal(err)
	}
	for _, esperada := range []string{
		"Data Descrição País Valor",
		"12/08 PADARIA EXEMPLO NATAL BR R$ 45,90",
		"30/12 LOJA ANTIGA PARC 09/10 NATAL BR R$ 80,00",
		"Total da Fatura R$ 497,95",
	} {
		if !slices.Contains(linhas, esperada) {
			t.Errorf("faltou a linha %q em %q", esperada, linhas)
		}
	}
	if slices.Index(linhas, "Data Descrição País Valor") > slices.Index(linhas, "Total da Fatura R$ 497,95") {
		t.Error("as linhas saíram fora da ordem das páginas")
	}
}

func TestLinhasDePDFQuebradoDevolveErroSemPanico(t *testing.T) {
	_, err := Linhas([]byte("%PDF-1.4\nisto não é um PDF de verdade"))
	if !errors.Is(err, ErrIlegivel) {
		t.Fatalf("esperava ErrIlegivel, obteve %v", err)
	}
	if EhPDF([]byte("date,title,amount")) {
		t.Error("CSV não é PDF")
	}
}
