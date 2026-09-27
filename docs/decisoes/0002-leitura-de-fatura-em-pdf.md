# ADR-0002: Leitura da fatura do cartão em PDF no importador

**Estado:** Aceita
**Data:** 2026-09-27

## Contexto

O importador em Go lê extratos em OFX e CSV, e até aqui usava só a biblioteca padrão, como registrei na ADR-0001. O
Banco do Brasil, porém, só entrega a fatura do cartão de crédito (Ourocard) em PDF. Sem ler PDF, o Fluxo fica sem as
compras no cartão desse banco, que é onde está a maior parte dos gastos de quem usa o cartão no dia a dia.

A biblioteca padrão do Go não extrai texto de PDF. Conferi o formato da fatura com uma versão mascarada do texto, em que
letras e números viram `A` e `9`, sem ler os lançamentos. Os lançamentos ficam numa tabela com as colunas Data,
Descrição, País e Valor, que pode continuar por várias páginas. A data vem sem o ano.

## Decisão

Uso a biblioteca `github.com/ledongthuc/pdf`, com a versão fixada no `go.mod`, para extrair o texto das páginas. Ela é
escrita só em Go e não traz outras dependências. A licença é a BSD de 3 cláusulas, a mesma do Go.

A biblioteca fica isolada no pacote `importador/pdftexto`, que devolve as linhas de texto de cima para baixo. O domínio
recebe só essas linhas: a regra de negócio da fatura fica em `importador/dominio/fatura.go`. O `arch-go.yml` proíbe o
domínio de importar a biblioteca, do mesmo jeito que já proibia `net/http`, `encoding/json` e `database/sql`.

O ano de cada lançamento sai da data de fechamento impressa na fatura. Um mês depois do mês de fechamento pertence ao ano
anterior, o que acontece com parcelas de compras antigas.

## Alternativas consideradas

| Alternativa | Por que descartei |
|---|---|
| Chamar o `pdftotext`, do Poppler | A imagem do importador deixaria de partir do `scratch` e passaria a depender de um programa externo instalado nela |
| Um serviço em Python só para ler PDF | Mais uma linguagem e mais um serviço para manter, para um trabalho que cabe num pacote |
| Ler a fatura por OCR | Mais lento e menos preciso, e o PDF do banco já traz o texto |
| Não aceitar a fatura do Banco do Brasil | Deixaria de fora os gastos no cartão, que são o centro do produto |

## Consequências

O importador passa a ter a primeira dependência externa. Se o banco mudar o layout da fatura, o parser pode parar de
reconhecer os lançamentos. Nesse caso a importação falha com uma mensagem clara, sem aceitar dados errados.

A biblioteca só acompanha a posição do texto pelo operador `Tm` do PDF. A fatura do Banco do Brasil usa esse operador,
mas o PDF de outro banco pode não usar. Por isso o parser atual reconhece só a fatura do Ourocard e recusa outros PDFs
com uma explicação.

Com a minha fatura real de agosto, o saldo anterior somado aos lançamentos lidos bateu com o total impresso no PDF.

## Como verificar

`mise run lint:go` termina com o arch-go em 100%, e `mise x -- go test ./...`, dentro de `services/`, lê a fatura
sintética de `importador/dominio/testdata/bb-fatura.pdf`.
