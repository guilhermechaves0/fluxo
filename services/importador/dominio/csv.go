package dominio

import (
	"encoding/csv"
	"fmt"
	"io"
	"strings"
	"time"
)

const (
	linhasDePreambulo = 15 // alguns bancos põem dados da conta antes do cabeçalho
	sem               = -1
	anoCurto          = 2
	seculo            = "20"
)

// layoutCSV diz em que coluna está cada campo. Coluna ausente vale sem (-1).
type layoutCSV struct {
	origem                      string
	cartao                      bool // na fatura do cartão, valor positivo é gasto
	data, descricao, detalhe    int
	valor, entrada, saida, tipo int
	id, parcela, categoria      int
}

var nomesDeColuna = map[string][]string{
	"data":      {"data de compra", "data lancamento", "data", "date", "data movimento", "data da transacao"},
	"descricao": {"titulo", "title", "historico", "lancamento", "estabelecimento", "descricao"},
	"detalhe":   {"detalhes", "descricao"},
	"valor":     {"valor", "amount", "valor em r", "valor r"},
	"entrada":   {"entrada r", "entrada", "credito", "valor credito"},
	"saida":     {"saida r", "saida", "debito", "valor debito"},
	"tipo":      {"tipo lancamento", "tipo de lancamento"},
	"id":        {"identificador"},
	"parcela":   {"parcela"},
	"categoria": {"categoria"},
}

func lerCSV(texto string) (Extrato, error) {
	linhas := strings.Split(texto, "\n")
	primeira := ""
	for i := 0; i < len(linhas) && i < linhasDePreambulo; i++ {
		if strings.TrimSpace(linhas[i]) == "" {
			continue
		}
		if primeira == "" {
			primeira = linhas[i]
		}
		for _, separador := range []rune{';', ','} {
			cabecalho, err := registros(linhas[i], separador, 0)
			if err != nil || len(cabecalho) == 0 {
				continue
			}
			if layout, ok := reconhecer(cabecalho[0].campos); ok {
				resto, err := registros(strings.Join(linhas[i+1:], "\n"), separador, i+1)
				if err != nil {
					return Extrato{}, fmt.Errorf("%w: CSV malformado (%v)", ErrFormatoDesconhecido, err)
				}
				return aplicar(layout, resto), nil
			}
		}
	}
	if extrato, ok := semCabecalho(linhas); ok {
		return extrato, nil
	}
	colunas := strings.Split(primeira, ";")
	if len(colunas) < 2 {
		colunas = strings.Split(primeira, ",")
	}
	return Extrato{}, fmt.Errorf("%w: o cabeçalho %q não tem colunas de data, descrição e valor que eu reconheça",
		ErrFormatoDesconhecido, strings.TrimSpace(strings.Join(colunas, ", ")))
}

type registro struct {
	campos []string
	linha  int
}

// registros lê o CSV guardando a linha de cada registro. deslocamento é o número de linhas
// que vêm antes do texto no arquivo.
func registros(texto string, separador rune, deslocamento int) ([]registro, error) {
	leitor := csv.NewReader(strings.NewReader(texto))
	leitor.Comma = separador
	leitor.FieldsPerRecord = -1
	leitor.LazyQuotes = true
	leitor.TrimLeadingSpace = true
	var lidos []registro
	for {
		campos, err := leitor.Read()
		if err == io.EOF {
			return lidos, nil
		}
		if err != nil {
			return nil, err
		}
		linha, _ := leitor.FieldPos(0)
		lidos = append(lidos, registro{campos: campos, linha: deslocamento + linha})
	}
}

func reconhecer(cabecalho []string) (layoutCSV, bool) {
	nomes := make([]string, len(cabecalho))
	for i, c := range cabecalho {
		nomes[i] = normalizar(c)
	}
	achar := func(campo string, exceto ...int) int {
		for _, candidato := range nomesDeColuna[campo] {
			for i, nome := range nomes {
				if nome == candidato && !contem(exceto, i) {
					return i
				}
			}
		}
		return sem
	}
	l := layoutCSV{data: achar("data"), descricao: achar("descricao"), valor: achar("valor"), entrada: achar("entrada"),
		saida: achar("saida"), tipo: achar("tipo"), id: achar("id"), parcela: achar("parcela"),
		categoria: achar("categoria")}
	l.detalhe = achar("detalhe", l.descricao)
	temValor := l.valor != sem || (l.entrada != sem && l.saida != sem)
	if l.data == sem || l.descricao == sem || !temValor {
		return layoutCSV{}, false
	}
	tem := func(nome string) bool { return contem(nomes, nome) }
	switch {
	case tem("date") && tem("title") && tem("amount"):
		l.origem, l.cartao = "Nubank (fatura do cartão)", true
	case tem("identificador") && tem("descricao") && tem("valor"):
		l.origem = "Nubank (conta)"
	case tem("data de compra") && (tem("nome no cartao") || tem("final do cartao")):
		l.origem, l.cartao = "C6 Bank (fatura do cartão)", true
	case tem("data lancamento") && tem("entrada r") && tem("saida r"):
		l.origem = "C6 Bank (conta)"
	case tem("dependencia origem") || (tem("lancamento") && tem("detalhes")):
		l.origem = "Banco do Brasil"
	default:
		l.origem = "CSV"
	}
	return l, true
}

func aplicar(l layoutCSV, linhas []registro) Extrato {
	extrato := Extrato{Formato: FormatoCSV, Origem: l.origem}
	vistos := map[string]int{}
	for _, r := range linhas {
		posicao := r.linha
		if vazia(r.campos) {
			continue
		}
		lancamento, motivo := l.lancamento(r.campos)
		if motivo != "" {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: posicao, Motivo: motivo})
			continue
		}
		if lancamento.IDExterno == "" {
			lancamento.IDExterno = idGerado(l.origem, lancamento, vistos)
		} else if vistos[lancamento.IDExterno] > 0 {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: posicao, Motivo: "identificador repetido"})
			continue
		}
		vistos[lancamento.IDExterno]++
		extrato.Lancamentos = append(extrato.Lancamentos, lancamento)
	}
	return extrato
}

func (l layoutCSV) lancamento(campos []string) (Lancamento, string) {
	campo := func(i int) string {
		if i == sem || i >= len(campos) {
			return ""
		}
		return strings.TrimSpace(campos[i])
	}
	descricao := campo(l.descricao)
	if d := campo(l.detalhe); d != "" && d != descricao {
		descricao = strings.TrimSpace(descricao + " - " + d)
	}
	if strings.Contains(normalizar(descricao), "saldo") || strings.Contains(normalizar(descricao), "s a l d o") {
		return Lancamento{}, "linha de saldo"
	}
	if p := campo(l.parcela); p != "" && !strings.HasPrefix(normalizar(p), "unica") {
		descricao += " (" + p + ")"
	}
	data, err := dataCSV(campo(l.data))
	if err != nil {
		return Lancamento{}, err.Error()
	}
	centavos, err := l.centavos(campo)
	if err != nil {
		return Lancamento{}, err.Error()
	}
	if centavos == 0 {
		return Lancamento{}, "valor zero"
	}
	tipo := Receita
	if (centavos < 0) != l.cartao {
		tipo = Despesa
	}
	if centavos < 0 {
		centavos = -centavos
	}
	switch t := normalizar(campo(l.tipo)); {
	case strings.HasPrefix(t, "saida") || strings.HasPrefix(t, "debito"):
		tipo = Despesa
	case strings.HasPrefix(t, "entrada") || strings.HasPrefix(t, "credito"):
		tipo = Receita
	}
	return Lancamento{IDExterno: campo(l.id), Data: data, ValorCentavos: centavos, Descricao: descricao, Tipo: tipo,
		Categoria: campo(l.categoria)}, ""
}

func (l layoutCSV) centavos(campo func(int) string) (int64, error) {
	if l.valor != sem && campo(l.valor) != "" {
		return Centavos(limparValor(campo(l.valor)))
	}
	if e := campo(l.entrada); e != "" {
		v, err := Centavos(limparValor(e))
		return abs(v), err
	}
	if s := campo(l.saida); s != "" {
		v, err := Centavos(limparValor(s))
		return -abs(v), err
	}
	return 0, fmt.Errorf("%w: sem valor", ErrValorInvalido)
}

// semCabecalho aceita o formato antigo "data;descricao;valor", sem linha de cabeçalho.
func semCabecalho(linhas []string) (Extrato, bool) {
	extrato := Extrato{Formato: FormatoCSV, Origem: "CSV"}
	for i, linha := range linhas {
		if strings.TrimSpace(linha) == "" {
			continue
		}
		partes := strings.Split(strings.TrimSpace(linha), ";")
		if len(partes) != camposCSV {
			return Extrato{}, false
		}
		data, err := dataCSV(partes[0])
		if err != nil {
			if len(extrato.Lancamentos) == 0 && i == 0 {
				return Extrato{}, false
			}
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: i + 1, Motivo: err.Error()})
			continue
		}
		lancamento, err := montar("", data, strings.TrimSpace(partes[1]), limparValor(partes[2]))
		if err != nil {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: i + 1, Motivo: err.Error()})
			continue
		}
		extrato.Lancamentos = append(extrato.Lancamentos, lancamento)
	}
	vistos := map[string]int{}
	for i := range extrato.Lancamentos {
		extrato.Lancamentos[i].IDExterno = idGerado(extrato.Origem, extrato.Lancamentos[i], vistos)
		vistos[extrato.Lancamentos[i].IDExterno]++
	}
	return extrato, len(extrato.Lancamentos) > 0
}

// dataCSV aceita dd/mm/aaaa, dd/mm/aa, dd-mm-aaaa e aaaa-mm-dd (com ou sem hora) e devolve aaaa-mm-dd.
func dataCSV(s string) (string, error) {
	s = strings.TrimSpace(s)
	if s == "" {
		return "", fmt.Errorf("%w: vazia", ErrDataInvalida)
	}
	if len(s) >= tamanhoDataISO && s[4] == '-' && s[7] == '-' {
		return validarData(s[:tamanhoDataISO])
	}
	partes := strings.FieldsFunc(strings.Fields(s + " ")[0], func(r rune) bool { return r == '/' || r == '-' })
	if len(partes) != 3 || len(partes[0]) > 2 || len(partes[1]) > 2 {
		return "", fmt.Errorf("%w: %q", ErrDataInvalida, s)
	}
	ano := partes[2]
	if len(ano) == anoCurto {
		ano = seculo + ano
	}
	return validarData(fmt.Sprintf("%s-%02s-%02s", ano, partes[1], partes[0]))
}

func validarData(iso string) (string, error) {
	iso = strings.ReplaceAll(iso, " ", "0")
	if len(iso) != tamanhoDataISO {
		return "", fmt.Errorf("%w: %q", ErrDataInvalida, iso)
	}
	for i, c := range iso {
		if (i == 4 || i == 7) != (c == '-') || (c != '-' && (c < '0' || c > '9')) {
			return "", fmt.Errorf("%w: %q", ErrDataInvalida, iso)
		}
	}
	// time.Parse recusa o que não existe no calendário, como 31/02 ou 29/02 fora de ano bissexto.
	if _, err := time.Parse(time.DateOnly, iso); err != nil {
		return "", fmt.Errorf("%w: %q", ErrDataInvalida, iso)
	}
	return iso, nil
}

func limparValor(s string) string {
	s = strings.NewReplacer("R$", "", " ", "", " ", "", "+", "").Replace(s)
	return strings.TrimSpace(s)
}

// normalizar deixa só letras minúsculas sem acento, dígitos e espaços simples, para
// comparar nomes de coluna: "Valor (em R$)" vira "valor em r".
func normalizar(s string) string {
	var b strings.Builder
	for _, r := range strings.ToLower(s) {
		if sub, ok := semAcento[r]; ok {
			r = sub
		}
		if (r >= 'a' && r <= 'z') || (r >= '0' && r <= '9') {
			b.WriteRune(r)
		} else {
			b.WriteRune(' ')
		}
	}
	return strings.Join(strings.Fields(b.String()), " ")
}

var semAcento = map[rune]rune{
	'á': 'a', 'à': 'a', 'â': 'a', 'ã': 'a', 'ä': 'a', 'é': 'e', 'ê': 'e', 'è': 'e', 'í': 'i', 'î': 'i',
	'ó': 'o', 'ô': 'o', 'õ': 'o', 'ö': 'o', 'ú': 'u', 'ü': 'u', 'ç': 'c', 'º': 'o', '°': 'o',
}

func vazia(campos []string) bool {
	for _, c := range campos {
		if strings.TrimSpace(c) != "" {
			return false
		}
	}
	return true
}

func contem[T comparable](lista []T, item T) bool {
	for _, x := range lista {
		if x == item {
			return true
		}
	}
	return false
}

func abs(v int64) int64 {
	if v < 0 {
		return -v
	}
	return v
}
