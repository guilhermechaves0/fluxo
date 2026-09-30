# Como rodar

## Pré-requisitos

- [mise](https://mise.jdx.dev), que instala as versões do JDK 21, do Go 1.27, do buf e do arch-go usadas no projeto
- SDK do Android com a plataforma 37 (`platforms;android-37.0`), instalado pelo Android Studio ou pelo
  `android-commandlinetools`
- Docker, para subir o banco, a api e o importador juntos e para os testes de integração da api
- Celular Android com depuração USB ativada ou um emulador

## Primeira execução

```bash
mise trust && mise install
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
mise run ci
```

O `local.properties` guarda o caminho do SDK na máquina e fica fora do repositório.

## Tarefas

| Comando | O que faz |
|---|---|
| `mise run build` | Compila a api, o app para desktop, o APK e o serviço Go |
| `mise run test` | Roda os testes das duas stacks. Os de integração da api sobem um PostgreSQL num container |
| `mise run lint` | Roda ktlint, detekt, gofmt, go vet, arch-go e buf lint |
| `mise run ci` | Roda lint, build e testes na mesma ordem do GitHub Actions |
| `mise run up` | Sobe o banco (porta 5432), a api (porta 8080) e o importador (porta 9090) com Docker Compose |
| `mise run banco` | Sobe só o PostgreSQL do Docker Compose |
| `mise run run:api` | Sobe o banco e roda a api pelo Gradle, sem container |
| `mise run run:importador` | Roda o importador sem container |
| `mise run run:app` | Abre o app no desktop |
| `mise run run:android` | Instala e abre o app no celular conectado por USB |
| `mise run run:deeplink t-03` | Abre no celular o detalhe da transação `t-03` pelo link `fluxo://transacao/t-03` |

## Banco de dados

A api grava no PostgreSQL 17. Ao subir, ela aplica as migrações de `api/src/main/resources/db/migration` com o
Flyway, então um banco vazio chega ao esquema atual sem nenhum passo manual. Os dados ficam no volume
`fluxo-db-dados`. Para começar do zero, use `docker compose down -v`.

A api lê o endereço do banco de variáveis de ambiente. Os valores padrão são os do banco do Docker Compose:

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fluxo` |
| `DB_USER` | `fluxo` |
| `DB_PASSWORD` | `fluxo` |
| `PORT` | `8080` |
| `FLUXO_IMPORTADOR_URL` | `http://localhost:9090` |

Se uma porta já estiver ocupada na máquina, dá para trocar só a porta do host no Docker Compose:

```bash
FLUXO_API_PORTA=8081 FLUXO_IMPORTADOR_PORTA=9091 FLUXO_DB_PORTA=5433 mise run up
```

## Testes sem Docker

Os testes de integração da api (`IntegracaoPostgresTest`) usam o Testcontainers e precisam do Docker. Numa máquina
sem Docker, este comando roda os outros testes da api:

```bash
./gradlew :api:test -PsemDocker
```

O CI roda todos.

## Conferir os serviços

Com o banco, a api e o importador no ar:

```bash
curl -s localhost:8080/health
curl -s localhost:9090/health
curl -s -i -X POST localhost:8080/contas -H 'Content-Type: application/json' -d '{"nome":"Nubank"}'
curl -s "localhost:8080/contas?pagina=0&tamanho=20&nome=nu"
```

O `POST` responde `201` com o cabeçalho `Location` da conta criada. Repetir o mesmo nome responde `409`, e um nome
vazio responde `422`, os dois no formato `application/problem+json`.

As transações ficam dentro da conta. Troque `ID_DA_CONTA` pelo `id` que o `POST` acima devolveu:

```bash
curl -s -i -X POST localhost:8080/contas/ID_DA_CONTA/transacoes -H 'Content-Type: application/json' \
  -d '{"descricao":"Supermercado","valorCentavos":31245,"data":"2026-09-06","tipo":"DESPESA","categoria":"Mercado"}'
curl -s "localhost:8080/contas/ID_DA_CONTA/transacoes?mes=2026-09&tipo=DESPESA&categoria=mercado&tamanho=20"
```

A listagem vem da data mais recente para a mais antiga e aceita os filtros `tipo`, `mes`, `categoria` e `descricao`.
Remover a conta remove as transações dela.
