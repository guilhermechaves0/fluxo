# Uso de ferramentas de IA

Registro exigido pelas sistemáticas de avaliação de DIM0524 e de DIM0547. Todo conteúdo produzido com ajuda de IA foi
revisado pelo autor, que responde por ele nas apresentações.

## Ferramentas

| Ferramenta | Para que foi usada |
|---|---|
| Assistente de programação com IA | Leitura dos repositórios das disciplinas, planejamento da sprint, primeira versão do código e dos textos, e revisão de redação |

## Registro da Sprint 0

| Parte do projeto | Como a IA foi usada | Como o resultado foi verificado |
|---|---|---|
| Planejamento | Leitura dos cronogramas, das rubricas e do projeto MUSI para montar o plano da sprint | Comparação do plano com as rubricas das duas disciplinas |
| Configuração do build, do CI e dos Dockerfiles | Geração dos arquivos a partir da estrutura do MUSI e do template oficial de Kotlin Multiplatform | `mise run ci` e execução do GitHub Actions |
| Código de `shared/`, `app/`, `api/` e `services/` | Primeira versão das classes, da tela, do parser de extratos, dos testes e dos comentários | Testes, ktlint, detekt, go vet e arch-go, além da leitura e dos ajustes do autor |
| Proposta, README, ADR e roteiro dos vídeos | Rascunho a partir das decisões tomadas pelo autor e revisão da redação | Leitura do autor com base nos guias da Sprint 0 |

## Decisões do autor

A ideia do produto, um assistente de finanças no estilo do Pierre sem Open Finance, partiu do autor. O MVP, a
plataforma e as stacks foram discutidos com a ferramenta, e a escolha final foi do autor. A gravação dos vídeos é do
próprio autor.
