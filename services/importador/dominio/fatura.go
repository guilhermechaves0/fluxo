package dominio

import (
	"fmt"
	"regexp"
	"strconv"
	"strings"
)

const origemFaturaBB = "Banco do Brasil (fatura do cartão)"

var (
	// "12/08 PADARIA EXEMPLO NATAL BR R$ 45,90" ou, sem país, "20/08 IOF - COMPRA NO EXTERIOR R$ 3,85".
	reLancamentoFatura = regexp.MustCompile(`^(\d{2})/(\d{2}) (.+?)(?: ([A-Z]{2}))? R\$ ?(-?[\d.]+,\d{2})$`)
	reFechamento       = regexp.MustCompile(`(?i)fatura\s+\S+\s+em\s+(\d{2})/(\d{2})/(\d{4})`)
	reDataCompleta     = regexp.MustCompile(`\b(\d{2})/(\d{2})/(\d{4})\b`)
)

// LerTextoDeFatura lê a fatura do cartão Ourocard (Banco do Brasil) a partir das linhas de texto
// do PDF. Os lançamentos ficam entre o cabeçalho "Data Descrição País Valor" e a linha "Total da
// Fatura", às vezes em várias páginas. As linhas trazem só dia e mês, então o ano sai da data de
// fechamento: mês depois do fechamento é do ano anterior, como numa parcela de dezembro numa
// fatura de agosto. Na fatura, valor positivo é gasto e negativo é pagamento ou estorno. O banco
// agrupa as compras em seções ("Restaurantes", "Serviços"), e o título da seção vira a categoria.
func LerTextoDeFatura(linhas []string) (Extrato, error) {
	texto := strings.Join(linhas, "\n")
	if !strings.Contains(strings.ToUpper(texto), "OUROCARD") {
		return Extrato{}, fmt.Errorf("%w: o PDF não é uma fatura do cartão do Banco do Brasil, único PDF que o "+
			"Fluxo lê por enquanto. Exporte o extrato em OFX ou CSV", ErrFormatoDesconhecido)
	}
	ano, mes, err := fechamento(texto)
	if err != nil {
		return Extrato{}, err
	}
	extrato := Extrato{Formato: FormatoPDF, Origem: origemFaturaBB}
	vistos := map[string]int{}
	dentro := false
	secao := ""
	for i, linha := range linhas {
		normal := normalizar(linha)
		switch {
		case !dentro:
			dentro = normal == "data descricao pais valor"
			continue
		case strings.HasPrefix(normal, "total da fatura"):
			return extrato, nil
		}
		m := reLancamentoFatura.FindStringSubmatch(linha)
		if m == nil {
			if tituloDeSecao(linha) {
				secao = strings.TrimSpace(linha)
			}
			continue // saldo anterior, cotação e valor em moeda estrangeira
		}
		lancamento, motivo := lancamentoDaFatura(m, ano, mes)
		lancamento.Categoria = secao
		if motivo != "" {
			extrato.Ignorados = append(extrato.Ignorados, Ignorado{Posicao: i + 1, Motivo: motivo})
			continue
		}
		lancamento.IDExterno = idGerado(origemFaturaBB, lancamento, vistos)
		vistos[lancamento.IDExterno]++
		extrato.Lancamentos = append(extrato.Lancamentos, lancamento)
	}
	if !dentro {
		return Extrato{}, fmt.Errorf("%w: não achei a tabela de lançamentos (Data, Descrição, País, Valor) na "+
			"fatura", ErrFormatoDesconhecido)
	}
	return extrato, nil
}

func lancamentoDaFatura(m []string, anoFechamento, mesFechamento int) (Lancamento, string) {
	dia, _ := strconv.Atoi(m[1])
	mes, _ := strconv.Atoi(m[2])
	ano := anoFechamento
	if mes > mesFechamento {
		ano--
	}
	data, err := validarData(fmt.Sprintf("%04d-%02d-%02d", ano, mes, dia))
	if err != nil {
		return Lancamento{}, err.Error()
	}
	centavos, err := Centavos(m[5])
	if err != nil {
		return Lancamento{}, err.Error()
	}
	if centavos == 0 {
		return Lancamento{}, "valor zero"
	}
	tipo := Despesa
	if centavos < 0 {
		tipo, centavos = Receita, -centavos
	}
	return Lancamento{Data: data, ValorCentavos: centavos, Descricao: strings.TrimSpace(m[3]), Tipo: tipo}, ""
}

// fechamento devolve o ano e o mês de fechamento da fatura ("Fatura fechada em 29/08/2026") ou,
// sem essa linha, os da primeira data completa do documento, que costuma ser o vencimento.
func fechamento(texto string) (int, int, error) {
	m := reFechamento.FindStringSubmatch(texto)
	if m == nil {
		m = reDataCompleta.FindStringSubmatch(texto)
	}
	if m == nil {
		return 0, 0, fmt.Errorf("%w: não achei a data de fechamento nem o vencimento da fatura", ErrFormatoDesconhecido)
	}
	mes, _ := strconv.Atoi(m[2])
	ano, _ := strconv.Atoi(m[3])
	return ano, mes, nil
}

const tamanhoMaximoDeSecao = 40

// tituloDeSecao reconhece linhas como "Restaurantes" ou "Compras parceladas": texto curto, sem
// números e sem valor. Saldo anterior, cotação e nome do titular com o final do cartão ficam de fora.
func tituloDeSecao(linha string) bool {
	linha = strings.TrimSpace(linha)
	if linha == "" || len(linha) > tamanhoMaximoDeSecao || strings.Contains(linha, "R$") ||
		strings.HasPrefix(normalizar(linha), "saldo") {
		return false
	}
	return !strings.ContainsAny(linha, "0123456789*$")
}
