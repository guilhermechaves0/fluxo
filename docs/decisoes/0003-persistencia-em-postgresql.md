# ADR-0003: Persistência em PostgreSQL, com Flyway e Exposed

**Estado:** Aceita
**Data:** 2026-09-30

## Contexto

Até a Sprint 0 a api não gravava nada. Ela devolvia três transações fixas em memória e repassava os extratos ao
importador. A proposta já previa contas com transações, e a Sprint 1 de DIM0547 pede o CRUD de duas entidades
relacionadas, com o esquema versionado por migrações e testes contra um banco de verdade, iguais na minha máquina e
no CI.

Duas restrições pesam na escolha. O domínio fica em `shared/`, que o aplicativo também compila, então as entidades não
podem herdar nada de biblioteca de banco. E a api vai para uma hospedagem gratuita de 512 MB na Sprint 3, com banco
gerenciado.

## Decisão

Uso o PostgreSQL 17, com o esquema criado só por migrações do Flyway, que a api aplica ao subir. O acesso aos dados usa
o Exposed no modo DSL, por trás de portas declaradas na camada de aplicação (`RepositorioDeContas`), com a
implementação em `adaptadores/persistencia`.

As outras escolhas que saem daqui:

| Assunto | Escolha |
|---|---|
| Modelo | Uma conta tem muitas transações. A `V1` cria `contas`; a tabela `transacoes` entra na migração seguinte, com chave estrangeira para `contas` |
| Identificador | UUID gerado pela api, guardado como texto |
| Nome da conta | Único sem diferenciar maiúsculas, por índice em `lower(nome)`. A repetição vira `409` |
| Remoção de conta | Leva junto as transações dela (`ON DELETE CASCADE`) |
| Categoria | Fica na própria transação, como id e nome. Ainda não tem tabela |
| Listagem | Paginação e filtros no SQL, com `LIMIT` e `OFFSET` e teto de 100 itens por página |
| Conexão | Pool do HikariCP com 5 conexões; o endereço e a senha vêm de variáveis de ambiente |
| Testes | Testcontainers com a mesma imagem do banco do Docker Compose |

## Alternativas consideradas

| Alternativa | Por que descartei |
|---|---|
| Esquema criado pelo Exposed (`SchemaUtils.create`) | As mudanças no banco ficariam sem histórico e sem revisão, e a rubrica recusa |
| Exposed no modo DAO, ou JPA com Hibernate | As entidades passariam a herdar classes da biblioteca. No modo DSL o domínio de `shared/` continua em Kotlin puro, e a conversão entre linha e entidade fica num lugar só |
| JDBC direto | Eu escreveria à mão a montagem de filtros opcionais e da paginação, que o DSL já resolve |
| H2 em memória nos testes | É outro banco. Tipos e códigos de erro diferem, e o teste passaria sem provar nada sobre o PostgreSQL |
| Conferir o nome repetido com um `SELECT` antes do `INSERT` | Duas requisições ao mesmo tempo passariam as duas pela consulta. O índice único decide sem essa brecha |
| Tabela de categorias desde já | Hoje a categoria é só o rótulo que vem do extrato. A tabela entra junto com as regras de categorização, numa migração nova |
| Impedir a remoção de uma conta com transações | Obrigaria a apagar transação por transação antes de remover a conta, que não é o que se espera num aplicativo de finanças |
| Identificador sequencial | O número revelaria quantos registros existem e não deixaria o aplicativo criar o identificador sem internet, o que a Sprint 3 vai pedir |
| Api que sobe sem banco | Só serviria a um deploy sem banco, e o deploy é da Sprint 3, já com banco gerenciado |

## Consequências

A mesma suíte roda na minha máquina e no CI sem configuração de banco, porque o endereço vem do container de teste.

Em compensação:

- A api não sobe mais sem o PostgreSQL. `mise run run:api` passou a subir o banco antes.
- Os testes de integração precisam do Docker. Sem ele, `./gradlew :api:test -PsemDocker` roda só os outros.
- Cada tabela é descrita em dois lugares, na migração e no objeto `Table` do Exposed. Se os dois divergirem, quem
  acusa é o teste de integração, e não o compilador.
- A paginação por `OFFSET` fica lenta em páginas muito distantes. Para o volume de um aplicativo pessoal isso não pesa.
- Remover uma conta apaga as transações dela sem volta. O aplicativo vai ter de pedir confirmação.
- O `lower()` do índice segue as regras de caixa do banco, que podem tratar letras acentuadas de forma diferente do
  Kotlin.

## Como verificar

- `IntegracaoPostgresTest` sobe um PostgreSQL 17 com o Testcontainers e confere o histórico do Flyway, o CRUD de
  contas, o conflito de nome, a paginação e o filtro no SQL e a restrição `CHECK` do nome.
- `ArquiteturaTest` falha se a camada de aplicação importar Exposed, Flyway, JDBC ou o pool, e se uma rota falar
  direto com o banco.
- Este comando não deve encontrar nada:

```bash
grep -rn "SchemaUtils" api/src
```
