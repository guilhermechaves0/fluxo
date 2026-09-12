# Fluxo

O Fluxo é o assistente de finanças pessoais para Android que desenvolvo como projeto integrador de duas disciplinas do
Bacharelado em Engenharia de Software da UFRN, em 2026.2. Ele importa o extrato que o banco já exporta, em OFX ou CSV, e
organiza os gastos por categoria, sem precisar de conexão por Open Finance.

Uso este mesmo repositório nas duas disciplinas, como no projeto de referência [MUSI](https://github.com/fmarquesfilho/musi).

| Disciplina | Pastas avaliadas |
|---|---|
| DIM0524 Desenvolvimento de Sistemas para Dispositivos Móveis | `app/`, `app-android/` e `shared/` |
| DIM0547 Desenvolvimento de Sistemas Web II | `api/`, `services/` e `protos/` |

## Equipe

| Nome | Matrícula | GitHub | Papel |
|---|---|---|---|
| Guilherme Chaves | 20250071106 | [@guilhermechaves0](https://github.com/guilhermechaves0) | Product Owner e desenvolvedor |

Faço o projeto sozinho e vou apresentar na coorte B, online. Declarei a integração entre as duas disciplinas na
[proposta](docs/proposta.md).

## Sprint 0

Nesta sprint, entrego a primeira tela do aplicativo, a api com dados em memória e o parser de extratos do serviço Go.

| Item | Link |
|---|---|
| Proposta | [docs/proposta.md](docs/proposta.md) |
| Quadro no GitHub Projects | [github.com/users/guilhermechaves0/projects/2](https://github.com/users/guilhermechaves0/projects/2) |
| Vídeo de DIM0524 (Móveis) | [preencher] |
| Vídeo de DIM0547 (Web II) | [preencher] |
| Decisões de arquitetura | [docs/decisoes/](docs/decisoes/) |
| Registro de uso de IA | [docs/uso-de-ia.md](docs/uso-de-ia.md) |

## Arquitetura

```
  app/ + app-android/   Compose Multiplatform no Android e no desktop (DIM0524)
          │  HTTP e JSON, a partir da Sprint 3
          ▼
  api/                  Ktor e Koin, com as regras de negócio e os dados (DIM0547)
          │  HTTP e JSON na Sprint 1, gRPC com o contrato de protos/ na Sprint 2
          ▼
  services/             importador de extratos OFX e CSV em Go (DIM0547)

  shared/               domínio em Kotlin, o mesmo no app e na api
```

## Como rodar

Para rodar o projeto, é preciso ter o [mise](https://mise.jdx.dev), o Docker e o SDK do Android instalados.

```bash
mise trust && mise install
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
mise run ci
```

| Comando | O que faz |
|---|---|
| `mise run run:app` | Abre o app no desktop |
| `mise run run:android` | Instala e abre o app no celular conectado por USB |
| `mise run up` | Sobe a api (porta 8080) e o importador (porta 9090) com Docker Compose |
| `mise run test` | Roda os testes das duas stacks |

O passo a passo completo está em [docs/COMO-RODAR.md](docs/COMO-RODAR.md).

## Checklist da Sprint 0

### DIM0524 (Móveis)
- [x] Repositório público, app compila e roda nos alvos Android e desktop
- [x] CI verde: ktlintCheck + detekt
- [x] docs/proposta.md com justificativa de plataforma-alvo e backend
- [x] 1 tela em Compose com componente próprio
- [x] Coorte (A/B), integração e intenção de multiplataforma declaradas
- [ ] Vídeo 5 min

### DIM0547 (Web II)
- [x] Monorepo público: api/ services/ protos/ docs/ mise.toml docker-compose.yml
- [x] mise run build && mise run test passam localmente
- [x] CI verde (build dos dois stacks)
- [x] docs/proposta.md com justificativa Ktor×Quarkus e serviço principal×Go
- [x] Coorte (A/B) e integração declaradas
- [ ] Vídeo 5 min

## Licença

O código está sob a licença [Apache 2.0](LICENSE).
