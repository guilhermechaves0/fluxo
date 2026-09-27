// Package pdftexto extrai as linhas de texto de um PDF, de cima para baixo, página por página.
// É o único pacote que depende da biblioteca de PDF (ADR 0002): o domínio recebe só as linhas
// e o arch-go impede que ele importe a biblioteca.
package pdftexto

import (
	"bytes"
	"errors"
	"fmt"
	"strings"

	"github.com/ledongthuc/pdf"
)

// Erros de leitura, que podem ser comparados com errors.Is.
var (
	ErrProtegido = errors.New("o PDF está protegido por senha")
	ErrIlegivel  = errors.New("não consegui ler o texto do PDF")
)

// EhPDF reconhece o PDF pela assinatura no começo do arquivo.
func EhPDF(conteudo []byte) bool {
	return bytes.HasPrefix(conteudo, []byte("%PDF"))
}

// Linhas devolve o texto de cada linha visual do documento, com as palavras separadas por um
// espaço. A biblioteca entra em pânico com alguns PDFs malformados; o recover transforma isso
// em ErrIlegivel.
func Linhas(conteudo []byte) (linhas []string, err error) {
	defer func() {
		if r := recover(); r != nil {
			linhas, err = nil, fmt.Errorf("%w: %v", ErrIlegivel, r)
		}
	}()
	leitor, err := pdf.NewReader(bytes.NewReader(conteudo), int64(len(conteudo)))
	if errors.Is(err, pdf.ErrInvalidPassword) {
		return nil, ErrProtegido
	}
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrIlegivel, err)
	}
	for n := 1; n <= leitor.NumPage(); n++ {
		pagina := leitor.Page(n)
		if pagina.V.IsNull() {
			continue
		}
		fileiras, err := pagina.GetTextByRow()
		if err != nil {
			return nil, fmt.Errorf("%w: página %d: %v", ErrIlegivel, n, err)
		}
		for _, fileira := range fileiras {
			var pedacos []string
			for _, texto := range fileira.Content {
				pedacos = append(pedacos, texto.S)
			}
			if linha := strings.Join(strings.Fields(strings.Join(pedacos, " ")), " "); linha != "" {
				linhas = append(linhas, linha)
			}
		}
	}
	return linhas, nil
}
