package dominio

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"regexp"
	"strconv"
	"strings"
	"unicode/utf8"
)

// Formato do arquivo de extrato.
type Formato string

// Formatos aceitos.
const (
	FormatoOFX Formato = "OFX"
	FormatoCSV Formato = "CSV"
)

// Extrato é o resultado da leitura de um arquivo inteiro.
type Extrato struct {
	Formato     Formato
	Origem      string // banco e tipo de extrato, quando dá para reconhecer
	Lancamentos []Lancamento
	Ignorados   []Ignorado
}

// Ignorado é um bloco OFX ou uma linha CSV que não virou lançamento, com o motivo.
// Posicao conta a partir de 1: é o número do bloco no OFX e o da linha no CSV.
type Ignorado struct {
	Posicao int
	Motivo  string
}

// Erros de arquivo, que podem ser comparados com errors.Is.
var (
	ErrArquivoVazio        = errors.New("arquivo vazio")
	ErrFormatoDesconhecido = errors.New("formato de extrato não reconhecido")
)

const tamanhoID = 16

var (
	reAberturaSTMTTRN  = regexp.MustCompile(`(?i)<STMTTRN>`)
	reFechamentoSTMTRN = regexp.MustCompile(`(?i)</STMTTRN>`)
	bancosPorCodigo    = map[string]string{"001": "Banco do Brasil", "260": "Nubank", "336": "C6 Bank"}
	bancosPorNome      = [][2]string{{"BANCO DO BRASIL", "Banco do Brasil"}, {"NU PAGAMENTOS", "Nubank"},
		{"NUBANK", "Nubank"}, {"C6", "C6 Bank"}}
)

// LerExtrato reconhece o formato pelo conteúdo e converte o arquivo em lançamentos.
// Aceita UTF-8 e Windows-1252, que é a codificação do OFX de vários bancos brasileiros.
func LerExtrato(conteudo []byte) (Extrato, error) {
	texto := paraUTF8(conteudo)
	texto = strings.TrimPrefix(texto, "\ufeff")
	texto = strings.ReplaceAll(texto, "\r\n", "\n")
	if strings.TrimSpace(texto) == "" {
		return Extrato{}, ErrArquivoVazio
	}
	maiusculo := strings.ToUpper(texto)
	if strings.Contains(maiusculo, "<OFX>") || strings.HasPrefix(strings.TrimSpace(maiusculo), "OFXHEADER") {
		return lerOFX(texto)
	}
	return lerCSV(texto)
}

func lerOFX(texto string) (Extrato, error) {
	extrato := Extrato{Formato: FormatoOFX, Origem: origemOFX(texto)}
	inicios := reAberturaSTMTTRN.FindAllStringIndex(texto, -1)
	if len(inicios) == 0 {
		return extrato, nil
	}
	vistos := map[string]int{}
	for i, inicio := range inicios {
		fim := len(texto)
		if i+1 < len(inicios) {
			fim = inicios[i+1][0]
		}
		bloco := texto[inicio[0]:fim]
		if f := reFechamentoSTMTRN.FindStringIndex(bloco); f != nil {
			bloco = bloco[:f[1]]
		}
		lancamento, err := ParseSTMTTRN(bloco)
		if err != nil {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: i + 1, Motivo: err.Error()})
			continue
		}
		if lancamento.IDExterno == "" {
			lancamento.IDExterno = idGerado(extrato.Origem, lancamento, vistos)
		} else if vistos[lancamento.IDExterno] > 0 {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: i + 1, Motivo: "FITID repetido no arquivo"})
			continue
		}
		vistos[lancamento.IDExterno]++
		extrato.Lancamentos = append(extrato.Lancamentos, lancamento)
	}
	return extrato, nil
}

// origemOFX usa o código do banco (BANKID) ou, sem ele, o nome da instituição (ORG).
func origemOFX(texto string) string {
	cabecalho := texto
	if i := reAberturaSTMTTRN.FindStringIndex(texto); i != nil {
		cabecalho = texto[:i[0]]
	}
	campos := camposOFX(cabecalho)
	codigo := strings.TrimLeft(campos["BANKID"], "0")
	if codigo == "" {
		codigo = strings.TrimLeft(campos["FID"], "0")
	}
	if n, err := strconv.Atoi(codigo); err == nil {
		if nome, ok := bancosPorCodigo[fmt.Sprintf("%03d", n)]; ok {
			return nome
		}
	}
	org := strings.ToUpper(campos["ORG"])
	for _, par := range bancosPorNome {
		if strings.Contains(org, par[0]) {
			return par[1]
		}
	}
	if campos["ORG"] != "" {
		return campos["ORG"]
	}
	return "OFX"
}

// idGerado cria um identificador estável para lançamento sem FITID, para que importar o
// mesmo arquivo de novo não duplique. Linhas iguais no mesmo arquivo ganham um contador.
func idGerado(origem string, l Lancamento, vistos map[string]int) string {
	base := fmt.Sprintf("%s|%s|%d|%s|%s", origem, l.Data, l.ValorCentavos, l.Tipo, l.Descricao)
	soma := sha256.Sum256([]byte(base))
	id := hex.EncodeToString(soma[:])[:tamanhoID]
	for vistos[id] > 0 {
		soma = sha256.Sum256([]byte(fmt.Sprintf("%s|%d", base, vistos[id])))
		id = hex.EncodeToString(soma[:])[:tamanhoID]
	}
	return id
}

// paraUTF8 devolve o texto como está quando ele já é UTF-8 e, senão, converte de
// Windows-1252, que coincide com o Latin-1 fora da faixa 0x80 a 0x9F.
func paraUTF8(conteudo []byte) string {
	if utf8.Valid(conteudo) {
		return string(conteudo)
	}
	var b bytes.Buffer
	for _, c := range conteudo {
		if c >= 0x80 && c <= 0x9F {
			b.WriteRune(windows1252[c-0x80])
			continue
		}
		b.WriteRune(rune(c))
	}
	return b.String()
}

var windows1252 = [32]rune{
	0x20AC, 0xFFFD, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, 0x02C6, 0x2030, 0x0160, 0x2039, 0x0152, 0xFFFD,
	0x017D, 0xFFFD, 0xFFFD, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, 0x02DC, 0x2122, 0x0161, 0x203A,
	0x0153, 0xFFFD, 0x017E, 0x0178,
}
