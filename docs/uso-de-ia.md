# Uso de ferramentas de IA

Mantenho este registro porque as sistemáticas de avaliação de DIM0524 e DIM0547 pedem que cada grupo informe as
ferramentas de IA usadas e as tarefas em que foram aplicadas. Respondo nas apresentações por todo o conteúdo produzido
com ajuda da ferramenta.

## Ferramentas

| Ferramenta | Para que usei |
|---|---|
| Assistente de programação com IA | Ler os repositórios das disciplinas, planejar a sprint, gerar a primeira versão do código e dos textos e revisar a redação |

## Registro da Sprint 0

| Parte do projeto | Como usei a IA | Como verifiquei |
|---|---|---|
| Planejamento | Para ler os cronogramas, as rubricas e o projeto MUSI e montar o plano da sprint | Comparação do plano com as rubricas das duas disciplinas |
| Configuração do build, do CI e dos Dockerfiles | Para gerar os arquivos a partir da estrutura do MUSI e do template oficial de Kotlin Multiplatform | `mise run ci` e execução do GitHub Actions |
| Código de `shared/`, `app/`, `api/` e `services/` | Para gerar a primeira versão das classes, da tela, do parser de extratos, dos testes e dos comentários | Testes, ktlint, detekt, go vet e arch-go no CI, além da leitura do código |
| Proposta, README, ADR e roteiro dos vídeos | Para rascunhar os textos a partir das minhas decisões e revisar a redação | Leitura e revisão dos textos com base nos guias da Sprint 0 |

## Registro da Sprint 1

| Parte do projeto | Como usei a IA | Como verifiquei |
|---|---|---|
| Navegação, formulário de lançamento e detalhe da transação | Para gerar a primeira versão das telas, das rotas e da validação a partir do exemplo `tarefas-compose` da disciplina | Testes de interface e de regra, ktlint e detekt, além do uso do app no celular |
| Deep link | Para declarar o link no grafo de navegação e no manifesto do Android | Link aberto no celular com `mise run run:deeplink` |
| Importação de extratos no importador em Go, na api e no app | Para gerar a primeira versão da leitura de OFX e CSV, das rotas e da tela de importação | Testes com extratos sintéticos de cada banco e importação dos meus extratos reais pelo celular |
| Leitura da fatura do cartão em PDF | Para escrever a leitura a partir de uma versão mascarada do texto da fatura, sem os lançamentos, e para rascunhar a ADR 0002 | Teste com uma fatura sintética e conferência de que a soma lida bate com o total da minha fatura real |
| Categoria do banco na importação | Para usar as seções da fatura do Banco do Brasil e a coluna Categoria do CSV do C6 como categoria | Testes com arquivos sintéticos e conferência na minha fatura real, olhando só os nomes das seções |
| Novo visual e layout em duas larguras | Para montar um plano de design a partir das minhas escolhas de tom, cor, layout e letra, e gerar a primeira versão do tema e das telas | Capturas das telas nos modos claro e escuro e nas duas larguras, testes de interface e uso no celular |
| Teste de arquitetura da api | Para escrever as regras de dependência com o ArchUnit, a partir do teste do projeto MUSI | Import de Ktor posto de propósito num caso de uso: o teste falhou com a mensagem da regra e voltou a passar sem o import |
| Banco de dados e CRUD de contas | Para gerar a primeira versão da migração, do repositório com Exposed, dos casos de uso, das rotas e dos testes, a partir do exemplo `ktor-tarefas` da disciplina e do projeto MUSI, e para rascunhar a ADR 0003 | Testes de unidade e de rota, testes de integração com um PostgreSQL no Testcontainers e chamadas com `curl` à api no Docker Compose, antes e depois de reiniciar o container |

## Decisões minhas

A ideia do produto, um assistente de finanças no estilo do Pierre sem Open Finance, foi minha. Discuti o MVP, a
plataforma e as stacks com a ferramenta, e as escolhas finais foram minhas. A gravação dos vídeos também é minha.
