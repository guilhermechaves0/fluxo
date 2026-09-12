# Como rodar

## Pré-requisitos

- [mise](https://mise.jdx.dev), que instala as versões do JDK 21, do Go 1.27, do buf e do arch-go usadas no projeto
- SDK do Android com a plataforma 37 (`platforms;android-37.0`), instalado pelo Android Studio ou pelo
  `android-commandlinetools`
- Docker, para subir a api e o importador juntos
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
| `mise run test` | Roda os testes das duas stacks |
| `mise run lint` | Roda ktlint, detekt, gofmt, go vet, arch-go e buf lint |
| `mise run ci` | Roda lint, build e testes na mesma ordem do GitHub Actions |
| `mise run up` | Sobe a api (porta 8080) e o importador (porta 9090) com Docker Compose |
| `mise run run:app` | Abre o app no desktop |
| `mise run run:android` | Instala e abre o app no celular conectado por USB |
| `mise run run:api` e `mise run run:importador` | Sobem cada serviço sem Docker |

## Conferir os serviços

Com a api e o importador no ar:

```bash
curl -s localhost:8080/health
curl -s localhost:8080/transacoes
curl -s localhost:9090/health
```
