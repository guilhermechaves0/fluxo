package pdftexto

import (
	"context"
	"errors"
	"os"
	"testing"
)

func TestLinhasRespeitaOContextoCancelado(t *testing.T) {
	conteudo, err := os.ReadFile("../dominio/testdata/bb-fatura.pdf")
	if err != nil {
		t.Fatal(err)
	}
	cancelado, cancelar := context.WithCancel(context.Background())
	cancelar()
	if _, err := Linhas(cancelado, conteudo); !errors.Is(err, context.Canceled) {
		t.Fatalf("esperava context.Canceled, obteve %v", err)
	}
}
