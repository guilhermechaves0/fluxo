package dominio

import (
	"context"
	"errors"
	"testing"
	"time"
)

// contextoQueVenceDepois fica válido para as primeiras restantes consultas de Err e depois passa a
// devolver o erro dado. Serve para provar que o laço de leitura consulta o contexto a cada passo,
// sem depender do relógio.
type contextoQueVenceDepois struct {
	context.Context
	restantes int
	erro      error
}

func (c *contextoQueVenceDepois) Err() error {
	if c.restantes > 0 {
		c.restantes--
		return nil
	}
	return c.erro
}

func TestLeituraRespeitaOPrazoEOCancelamento(t *testing.T) {
	vencido, cancelarVencido := context.WithTimeout(context.Background(), -time.Second)
	defer cancelarVencido()
	cancelado, cancelar := context.WithCancel(context.Background())
	cancelar()
	semCabecalho := []byte("2026-09-06;Supermercado;-312,45\n05/09/2026;Salário;4500.00\n")

	casos := []struct {
		nome     string
		ctx      context.Context
		conteudo []byte
		esperado error
	}{
		{"prazo vencido antes de começar", vencido, ler(t, "nubank-conta.ofx"), context.DeadlineExceeded},
		{"chamada cancelada antes de começar", cancelado, ler(t, "nubank-conta.ofx"), context.Canceled},
		{"prazo vence no meio do OFX", &contextoQueVenceDepois{context.Background(), 3, context.DeadlineExceeded},
			ler(t, "nubank-conta.ofx"), context.DeadlineExceeded},
		{"cancelamento no meio do CSV", &contextoQueVenceDepois{context.Background(), 4, context.Canceled},
			ler(t, "c6-fatura.csv"), context.Canceled},
		{"prazo vence no CSV sem cabeçalho", &contextoQueVenceDepois{context.Background(), 2, context.DeadlineExceeded},
			semCabecalho, context.DeadlineExceeded},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			extrato, err := LerExtrato(c.ctx, c.conteudo)
			if !errors.Is(err, c.esperado) {
				t.Fatalf("esperava %v na cadeia de erros, obteve %v", c.esperado, err)
			}
			if len(extrato.Lancamentos) != 0 {
				t.Errorf("leitura interrompida não devolve lançamentos pela metade: %+v", extrato.Lancamentos)
			}
		})
	}
}

func TestLeituraDaFaturaRespeitaOPrazo(t *testing.T) {
	ctx := &contextoQueVenceDepois{context.Background(), 3, context.DeadlineExceeded}
	if _, err := LerTextoDeFatura(ctx, faturaDeExemplo); !errors.Is(err, context.DeadlineExceeded) {
		t.Fatalf("esperava context.DeadlineExceeded, obteve %v", err)
	}
}
