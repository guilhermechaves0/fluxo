// Serviço importador do Fluxo (DIM0547).
//
// Recebe extratos bancários em OFX ou CSV e devolve os lançamentos. Expõe /health e
// POST /importar, que lê o corpo da requisição com o parser de importador/dominio. Usa
// apenas a biblioteca padrão.
package main

import (
	"encoding/json"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/guilhermechaves0/fluxo/services/importador/dominio"
)

const tamanhoMaximo = 5 << 20 // 5 MB: um extrato de vários anos ainda cabe com folga

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
	mux.HandleFunc("POST /importar", importar)
	return mux
}

// importar lê o arquivo inteiro do corpo e responde com os lançamentos e as linhas ignoradas.
// O conteúdo do extrato não vai para o log, só as contagens.
func importar(w http.ResponseWriter, r *http.Request) {
	conteudo, err := io.ReadAll(http.MaxBytesReader(w, r.Body, tamanhoMaximo))
	var grande *http.MaxBytesError
	if errors.As(err, &grande) {
		problema(w, http.StatusRequestEntityTooLarge, "extrato-grande-demais", "O extrato passa de 5 MB.")
		return
	}
	if err != nil {
		problema(w, http.StatusBadRequest, "corpo-invalido", "Não consegui ler o corpo da requisição.")
		return
	}
	extrato, err := dominio.LerExtrato(conteudo)
	switch {
	case errors.Is(err, dominio.ErrArquivoVazio):
		problema(w, http.StatusBadRequest, "extrato-vazio", "O arquivo está vazio.")
		return
	case err != nil:
		problema(w, http.StatusUnprocessableEntity, "extrato-nao-reconhecido", err.Error())
		return
	}
	slog.Info("extrato lido", "formato", extrato.Formato, "origem", extrato.Origem,
		"lancamentos", len(extrato.Lancamentos), "ignorados", len(extrato.Ignorados))
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(paraResposta(extrato))
}

type resposta struct {
	Formato     string           `json:"formato"`
	Origem      string           `json:"origem"`
	Lancamentos []lancamentoJSON `json:"lancamentos"`
	Ignorados   []ignoradoJSON   `json:"ignorados"`
}

type lancamentoJSON struct {
	IDExterno     string `json:"idExterno"`
	Data          string `json:"data"`
	ValorCentavos int64  `json:"valorCentavos"`
	Descricao     string `json:"descricao"`
	Tipo          string `json:"tipo"`
}

type ignoradoJSON struct {
	Posicao int    `json:"posicao"`
	Motivo  string `json:"motivo"`
}

func paraResposta(e dominio.Extrato) resposta {
	r := resposta{Formato: string(e.Formato), Origem: e.Origem, Lancamentos: []lancamentoJSON{},
		Ignorados: []ignoradoJSON{}}
	for _, l := range e.Lancamentos {
		r.Lancamentos = append(r.Lancamentos, lancamentoJSON{IDExterno: l.IDExterno, Data: l.Data,
			ValorCentavos: l.ValorCentavos, Descricao: l.Descricao, Tipo: string(l.Tipo)})
	}
	for _, i := range e.Ignorados {
		r.Ignorados = append(r.Ignorados, ignoradoJSON{Posicao: i.Posicao, Motivo: i.Motivo})
	}
	return r
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
