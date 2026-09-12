// Serviço importador do Fluxo (DIM0547).
//
// Recebe extratos bancários em OFX ou CSV e devolve os lançamentos. Por enquanto expõe
// /health e POST /importar, que responde 501; o parser de importador/dominio passa a ser
// usado por essa rota na Sprint 1. Usa apenas a biblioteca padrão.
package main

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"os"
	"time"
)

func main() {
	porta := os.Getenv("PORT")
	if porta == "" {
		porta = "9090"
	}

	srv := &http.Server{
		Addr:              ":" + porta,
		Handler:           rotas(),
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       30 * time.Second,
		WriteTimeout:      30 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	slog.Info("importador no ar", "porta", porta)
	if err := srv.ListenAndServe(); err != nil {
		slog.Error("servidor encerrou", "erro", err)
		os.Exit(1)
	}
}

// rotas monta o roteador. Fica fora do main para que os testes não precisem abrir uma porta.
func rotas() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"status":"UP"}`))
	})
	mux.HandleFunc("POST /importar", func(w http.ResponseWriter, _ *http.Request) {
		problema(w, http.StatusNotImplemented, "nao-implementado",
			"A importação de extratos entra na Sprint 1. O parser já existe em importador/dominio.")
	})
	return mux
}

// problema escreve a resposta de erro no formato da RFC 9457 (application/problem+json).
func problema(w http.ResponseWriter, status int, tipo, detalhe string) {
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]any{
		"type":   "https://fluxo.ufrn.br/erros/" + tipo,
		"status": status,
		"detail": detalhe,
	})
}
