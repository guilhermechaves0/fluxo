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

## Decisões minhas

A ideia do produto, um assistente de finanças no estilo do Pierre sem Open Finance, foi minha. Discuti o MVP, a
plataforma e as stacks com a ferramenta, e as escolhas finais foram minhas. A gravação dos vídeos também é minha.
