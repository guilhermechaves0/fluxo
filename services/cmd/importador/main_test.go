package main

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestRotas(t *testing.T) {
	casos := []struct {
		nome   string
		metodo string
		rota   string
		status int
	}{
		{"health responde UP", http.MethodGet, "/health", http.StatusOK},
		{"importar ainda não implementado", http.MethodPost, "/importar", http.StatusNotImplemented},
		{"método errado no health", http.MethodPost, "/health", http.StatusMethodNotAllowed},
		{"rota inexistente", http.MethodGet, "/nada", http.StatusNotFound},
	}
	mux := rotas()
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			rec := httptest.NewRecorder()
			mux.ServeHTTP(rec, httptest.NewRequest(c.metodo, c.rota, nil))
			if rec.Code != c.status {
				t.Errorf("%s %s: esperava %d, obteve %d", c.metodo, c.rota, c.status, rec.Code)
			}
		})
	}
}
