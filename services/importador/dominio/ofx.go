// Package dominio converte extratos bancários (OFX 1.x em SGML e CSV) em lançamentos.
// É a camada de domínio do importador e não pode importar net/http, encoding/json nem
// database/sql. O arch-go confere essa regra no CI (services/arch-go.yml).
package dominio

import (
	"errors"
	"fmt"
	"strconv"
	"strings"
)

// Tipo indica se o lançamento é receita ou despesa. O valor é sempre positivo.
type Tipo string

// Valores possíveis de Tipo.
const (
	Receita Tipo = "RECEITA"
	Despesa Tipo = "DESPESA"
)

// Lancamento é uma linha de extrato já convertida. Corresponde à mensagem Lancamento de
// protos/fluxo/importador/v1/importador.proto.
type Lancamento struct {
	IDExterno     string
	Data          string // AAAA-MM-DD
	ValorCentavos int64
	Descricao     string
	Tipo          Tipo
}

// Erros de interpretação, que podem ser comparados com errors.Is.
var (
	ErrCampoAusente  = errors.New("campo obrigatório ausente")
	ErrValorInvalido = errors.New("valor inválido")
	ErrDataInvalida  = errors.New("data inválida")
)

const (
	tamanhoDataOFX = 8  // AAAAMMDD
	tamanhoDataISO = 10 // AAAA-MM-DD
	camposCSV      = 3  // data;descricao;valor
	casasDecimais  = 2
)

// ParseSTMTTRN lê um bloco <STMTTRN>...</STMTTRN> do OFX 1.x, em que os campos não têm
// tag de fechamento. Usa os campos TRNAMT, DTPOSTED, FITID e MEMO.
func ParseSTMTTRN(bloco string) (Lancamento, error) {
	campos := map[string]string{}
	for _, linha := range strings.Split(bloco, "\n") {
		linha = strings.TrimSpace(linha)
		if !strings.HasPrefix(linha, "<") || strings.HasPrefix(linha, "</") {
			continue
		}
		fim := strings.Index(linha, ">")
		if fim < 0 {
			continue
		}
		campos[strings.ToUpper(linha[1:fim])] = strings.TrimSpace(linha[fim+1:])
	}
	if campos["TRNAMT"] == "" || campos["DTPOSTED"] == "" {
		return Lancamento{}, fmt.Errorf("%w: TRNAMT e DTPOSTED", ErrCampoAusente)
	}
	data, err := dataOFX(campos["DTPOSTED"])
	if err != nil {
		return Lancamento{}, err
	}
	return montar(campos["FITID"], data, campos["MEMO"], campos["TRNAMT"])
}

// ParseLinhaCSV lê uma linha no formato "data;descricao;valor", com data AAAA-MM-DD e
// valor com sinal. O separador decimal pode ser vírgula ou ponto.
func ParseLinhaCSV(linha string) (Lancamento, error) {
	partes := strings.Split(strings.TrimSpace(linha), ";")
	if len(partes) != camposCSV {
		return Lancamento{}, fmt.Errorf("%w: esperava data;descricao;valor", ErrCampoAusente)
	}
	data := strings.TrimSpace(partes[0])
	if len(data) != tamanhoDataISO || data[4] != '-' || data[7] != '-' {
		return Lancamento{}, fmt.Errorf("%w: %q", ErrDataInvalida, data)
	}
	return montar("", data, strings.TrimSpace(partes[1]), partes[2])
}

func montar(id, data, descricao, valor string) (Lancamento, error) {
	centavos, err := Centavos(valor)
	if err != nil {
		return Lancamento{}, err
	}
	tipo := Receita
	if centavos < 0 {
		tipo = Despesa
		centavos = -centavos
	}
	return Lancamento{IDExterno: id, Data: data, ValorCentavos: centavos, Descricao: descricao, Tipo: tipo}, nil
}

// dataOFX converte "20260905120000[-3:BRT]" em "2026-09-05".
func dataOFX(s string) (string, error) {
	if len(s) < tamanhoDataOFX {
		return "", fmt.Errorf("%w: %q", ErrDataInvalida, s)
	}
	for _, c := range s[:tamanhoDataOFX] {
		if c < '0' || c > '9' {
			return "", fmt.Errorf("%w: %q", ErrDataInvalida, s)
		}
	}
	return s[0:4] + "-" + s[4:6] + "-" + s[6:8], nil
}

// Centavos converte "-1234.56" (OFX) ou "1.234,56" (formato brasileiro) em centavos.
// A conta usa inteiros para evitar erro de arredondamento de ponto flutuante.
func Centavos(s string) (int64, error) {
	s = strings.TrimSpace(s)
	if s == "" {
		return 0, fmt.Errorf("%w: vazio", ErrValorInvalido)
	}
	negativo := strings.HasPrefix(s, "-")
	s = strings.TrimPrefix(s, "-")
	// Se houver vírgula, ela separa os decimais e os pontos separam os milhares.
	if strings.Contains(s, ",") {
		s = strings.ReplaceAll(s, ".", "")
		s = strings.Replace(s, ",", ".", 1)
	}
	inteiro, fracao, _ := strings.Cut(s, ".")
	if len(fracao) > casasDecimais {
		return 0, fmt.Errorf("%w: mais de duas casas decimais em %q", ErrValorInvalido, s)
	}
	fracao = (fracao + "00")[:casasDecimais]
	i, err := strconv.ParseInt(inteiro, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%w: %q", ErrValorInvalido, s)
	}
	f, err := strconv.ParseInt(fracao, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%w: %q", ErrValorInvalido, s)
	}
	valor := i*100 + f
	if negativo {
		valor = -valor
	}
	return valor, nil
}
