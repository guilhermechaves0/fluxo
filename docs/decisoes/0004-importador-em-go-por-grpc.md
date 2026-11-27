# ADR-0004: Importador de extratos em Go, chamado pela api por gRPC

**Estado:** Aceita
**Data:** 2026-11-27

## Contexto

O importador existe desde a Sprint 0. É um serviço em Go que recebe o arquivo exportado pelo banco (OFX, CSV ou a
fatura em PDF do Banco do Brasil) e devolve os lançamentos. Até a Sprint 1 a api o chamava por HTTP, com o JSON
descrito à mão dos dois lados.

A Sprint 2 de DIM0547 pede um serviço Go com uma responsabilidade que justifique ficar fora da api, um contrato em
Protocol Buffers de onde as duas pontas geram o código, e a api de pé quando o serviço Go não responde.

No Fluxo, o trabalho que cabe nessa descrição é a leitura de extratos. Cada banco exporta de um jeito, o PDF precisa
ser descomprimido e varrido, e o arquivo é entrada de terceiros, que pode vir malformada ou grande demais. O resto da
api é CRUD sobre o PostgreSQL.

## Decisão

O importador continua como um serviço separado, em Go, com uma responsabilidade: transformar o arquivo do banco em
lançamentos e na lista das linhas que ele ignorou. Ele não grava nada e não acessa o banco de dados. Quem decide o que
entra é a api, depois que o usuário confirma a prévia.

A api chama o importador por gRPC, pelo contrato `protos/fluxo/importador/v1/importador.proto`.

| Assunto | Escolha |
|---|---|
| Contrato | Pacote `fluxo.importador.v1`, com `buf lint` nas regras `STANDARD` e `COMMENTS` |
| Stubs | Os de Go são gerados pelo `buf generate` e versionados em `services/gen`. Os de Kotlin são gerados no build da api, pelo plugin `com.google.protobuf` |
| Prazo | Toda chamada da api leva um prazo de 10 segundos, que chega ao laço de leitura no Go pelo `context` |
| Erros | `INVALID_ARGUMENT` vira 422 e `RESOURCE_EXHAUSTED` vira 413. Todo o resto, inclusive prazo vencido e serviço fora do ar, vira 503 |
| Tamanho | Extrato de até 5 MB. O servidor aceita mensagens de até 6 MB, para recusar o excesso com o erro certo |
| Saúde | Health check do gRPC, conferido pelo subcomando `importador saude`, que o Docker Compose usa |
| Dependências | O domínio do importador segue só com a biblioteca padrão. gRPC e Protobuf ficam no adaptador `grpcapi` e em `cmd` |

## Alternativas consideradas

| Alternativa | Por que descartei |
|---|---|
| Ler os extratos dentro da api, em Kotlin | Um PDF pesado ou um arquivo malformado disputaria memória e threads com o CRUD. Separado, o pior caso é a importação responder 503 |
| Manter HTTP e JSON entre os dois | O formato da resposta ficava escrito duas vezes, em Go e em Kotlin, sem nada que acusasse a diferença. Com o `.proto`, quem acusa é o compilador |
| Importador gravando direto no banco | Os dois serviços passariam a dividir tabelas, e uma migração da api poderia quebrar o Go. A api continua dona dos dados |
| Fila entre a api e o importador | A prévia aparece na hora, para o usuário confirmar. Uma fila traria um componente a mais para devolver a mesma resposta com atraso |
| Dar outra responsabilidade ao serviço Go, como a categorização | A categorização automática ainda não existe no produto. A leitura de extratos já existia e é o trabalho mais pesado |

## Consequências

O contrato fica num lugar só, e mudar um campo obriga a gerar os stubs de novo nos dois lados.

A api não depende do importador para ficar de pé. Com ele parado, a importação responde 503, e as contas e as
transações continuam funcionando.

Em compensação:

- O módulo `services/` deixou de usar só a biblioteca padrão: o adaptador depende de `grpc` e de `protobuf`. O domínio
  continua sem dependência nenhuma, e o arch-go confere.
- O build da api passou a gerar código, e o CI confere se os stubs de Go versionados estão em dia com o contrato.
- Com o importador fora do ar, as primeiras respostas 503 demoram o prazo inteiro, porque o canal ainda tenta
  reconectar.

## Como verificar

- `buf lint` e a checagem dos stubs no CI.
- `go test ./...` em `services/`: o servidor pela pilha gRPC, com `bufconn`, e o teste de prazo do domínio.
- `ImportadorGrpcTest` na api, contra um servidor gRPC em memória: o sucesso, cada erro e o prazo.
- `ArquiteturaTest`: `io.grpc` e o código gerado são proibidos no domínio e na aplicação.
- `arch-go`: o domínio do importador não importa gRPC nem Protobuf.
