package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
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
		{"importar sem corpo", http.MethodPost, "/importar", http.StatusBadRequest},
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

func TestImportar(t *testing.T) {
	ofx, err := os.ReadFile("../../importador/dominio/testdata/nubank-conta.ofx")
	if err != nil {
		t.Fatal(err)
	}
	casos := []struct {
		nome         string
		corpo        string
		status       int
		tipoConteudo string
	}{
		{"extrato do Nubank", string(ofx), http.StatusOK, "application/json"},
		{"formato desconhecido", "coluna1;coluna2\nx;y\n", http.StatusUnprocessableEntity, "application/problem+json"},
		{"arquivo vazio", "   ", http.StatusBadRequest, "application/problem+json"},
		{"grande demais", strings.Repeat("x", tamanhoMaximo+1), http.StatusRequestEntityTooLarge,
			"application/problem+json"},
	}
	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			rec := httptest.NewRecorder()
			rotas().ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/importar", strings.NewReader(c.corpo)))
			if rec.Code != c.status || rec.Header().Get("Content-Type") != c.tipoConteudo {
				t.Fatalf("esperava %d %s, obteve %d %s: %s", c.status, c.tipoConteudo, rec.Code,
					rec.Header().Get("Content-Type"), rec.Body.String())
			}
		})
	}
}

func TestImportarDevolveOsLancamentos(t *testing.T) {
	ofx, _ := os.ReadFile("../../importador/dominio/testdata/nubank-conta.ofx")
	rec := httptest.NewRecorder()
	rotas().ServeHTTP(rec, httptest.NewRequest(http.MethodPost, "/importar", strings.NewReader(string(ofx))))
	var r resposta
	if err := json.NewDecoder(rec.Body).Decode(&r); err != nil {
		t.Fatal(err)
	}
	if r.Formato != "OFX" || r.Origem != "Nubank" || len(r.Lancamentos) != 3 || len(r.Ignorados) != 1 {
		t.Fatalf("resposta inesperada: %+v", r)
	}
	if r.Lancamentos[1].Tipo != "DESPESA" || r.Lancamentos[1].ValorCentavos != 4590 {
		t.Errorf("segundo lançamento errado: %+v", r.Lancamentos[1])
	}
}
