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
| Vídeo de DIM0524 (Móveis) | [youtu.be/8J29oE8NZKk](https://youtu.be/8J29oE8NZKk) |
| Vídeo de DIM0547 (Web II) | [youtu.be/PD6YycyKgCg](https://youtu.be/PD6YycyKgCg) |
| Decisões de arquitetura | [docs/decisoes/](docs/decisoes/) |
| Registro de uso de IA | [docs/uso-de-ia.md](docs/uso-de-ia.md) |

## Sprint 1

Nesta sprint, o app ganhou as telas do MVP, ligadas por um grafo de navegação com rotas tipadas e um deep link, o
tema claro e escuro, o layout em duas larguras e a leitura por TalkBack. A api passou a gravar contas e transações
num PostgreSQL com migrações do Flyway, com validação, erros em problem details e a documentação OpenAPI gerada das
rotas. A importação de extrato, que era da Sprint 2, veio antes: o app envia o arquivo para a api, e o importador em
Go lê OFX, CSV e a fatura em PDF do Banco do Brasil.

| Item | Link |
|---|---|
| Quadro da sprint | [github.com/users/guilhermechaves0/projects/2](https://github.com/users/guilhermechaves0/projects/2/views/1?filterQuery=sprint%3A1) |
| Vídeo de DIM0524 (Móveis) | [youtu.be/QDT6kR2T33U](https://youtu.be/QDT6kR2T33U) |
| Vídeo de DIM0547 (Web II) | [youtu.be/yrVUfd3lPBk](https://youtu.be/yrVUfd3lPBk) |
| Decisões de arquitetura | [docs/decisoes/](docs/decisoes/), com a ADR 0003 sobre a persistência |
| Documentação da api | Swagger UI em `/swagger` e OpenAPI em `/openapi.json`, com a api no ar |
| Registro de uso de IA | [docs/uso-de-ia.md](docs/uso-de-ia.md) |

### Deep link

`fluxo://transacao/{id}` abre o detalhe de uma transação. Com o app instalado no celular e uma transação lançada (a
primeira recebe o id `manual-1`):

```bash
mise run run:deeplink manual-1
```

O Voltar leva para a lista, e não para fora do app, porque a navegação monta a pilha a partir do destino inicial.

### Acessibilidade

O que conferi e ajustei nesta sprint:

- As cores vêm só dos papéis do tema. Os pares de texto e fundo ficam acima de 4,9:1 de contraste nos dois modos. O
  contorno dos chips e dos campos estava em 1,9:1 e subiu para mais de 3:1.
- Todo elemento interativo sem texto tem descrição: os botões de mês, o botão de adicionar e a barra de gastos por
  categoria, que o leitor de tela lê por extenso.
- O valor de cada transação e o total do dia passaram a ser lidos como gasto ou entrada, porque a diferença estava só
  na cor e no sinal.
- Os títulos de cada tela são marcados como cabeçalho, e a troca de mês e os passos da importação são anunciados.
- Os alvos de toque têm pelo menos 48 dp: os componentes do Material 3 garantem, e as linhas da lista e do painel de
  categorias têm 48 dp ou mais de altura.
- Teste com o TalkBack num Galaxy A54, em 16/10: percorri a lista, o detalhe, o formulário e a importação com o
  leitor ligado, comparando o que ele falava com o roteiro de leituras esperadas de cada elemento. Nada divergiu.

## Arquitetura

```
  app/ + app-android/   Compose Multiplatform no Android e no desktop (DIM0524)
          │  HTTP e JSON: a prévia de importação já usa a api; o CRUD vem na Sprint 2
          ▼
  api/                  Ktor e Koin, com as regras de negócio e o PostgreSQL (DIM0547)
          │  HTTP e JSON na Sprint 1, gRPC com o contrato de protos/ na Sprint 2
          ▼
  services/             importador de extratos OFX, CSV e PDF em Go (DIM0547)

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
| `mise run up` | Sobe o banco (porta 5432), a api (porta 8080) e o importador (porta 9090) com Docker Compose |
| `mise run test` | Roda os testes das duas stacks, com um PostgreSQL num container para os de integração |

Com a api no ar, a documentação das rotas fica na Swagger UI, em http://localhost:8080/swagger.

O passo a passo completo está em [docs/COMO-RODAR.md](docs/COMO-RODAR.md).

## Checklist da Sprint 0

### DIM0524 (Móveis)
- [x] Repositório público, app compila e roda nos alvos Android e desktop
- [x] CI verde: ktlintCheck + detekt
- [x] docs/proposta.md com justificativa de plataforma-alvo e backend
- [x] 1 tela em Compose com componente próprio
- [x] Coorte (A/B), integração e intenção de multiplataforma declaradas
- [x] Vídeo 5 min

### DIM0547 (Web II)
- [x] Monorepo público: api/ services/ protos/ docs/ mise.toml docker-compose.yml
- [x] mise run build && mise run test passam localmente
- [x] CI verde (build dos dois stacks)
- [x] docs/proposta.md com justificativa Ktor×Quarkus e serviço principal×Go
- [x] Coorte (A/B) e integração declaradas
- [x] Vídeo 5 min

## Checklist da Sprint 1

### DIM0524 (Móveis)
- [x] Todas as telas do MVP
- [x] Navigation Compose: rotas tipadas, com argumento, e um deep link
- [x] Material 3 com modo claro e escuro
- [x] Layout adaptado a dois tamanhos de janela, sem quebra
- [x] Acessibilidade: descrição de conteúdo, cores pelos papéis do tema, alvos de 48 dp e teste com o TalkBack
- [x] Formulário com validação
- [x] Vídeo 5 min

### DIM0547 (Web II)
- [x] CRUD de duas entidades com relacionamento
- [x] Camadas separadas: o domínio não importa framework nem infraestrutura
- [x] Migrações Flyway, sem geração automática de schema
- [x] Validação, problem details e OpenAPI
- [x] Vídeo 5 min

## Licença

O código está sob a licença [Apache 2.0](LICENSE).
