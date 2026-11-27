# ADR-0005: Implantação com imagens no GHCR, a api no Render e o banco no Neon

**Estado:** Aceita
**Data:** 2026-11-27

## Contexto

A Sprint 2 de DIM0547 pede a api e um banco gerenciado implantados numa plataforma de container, com as migrações
aplicadas por task ou pelo CI, a connection string em segredo, health check e a api respondendo quando o serviço Go
não está acessível. Desde o ajuste de 19/11 a URL pública deixou de ser exigida, e a implantação é avaliada pelas
evidências.

Faço o projeto sozinho e sem orçamento, então a implantação precisa caber em planos gratuitos. O guia da disciplina
usa o Render e o Neon e registra os limites do plano gratuito do Render: serviço privado é pago, um web service
gratuito não recebe tráfego pela rede interna, o endereço público não encaminha gRPC e o comando que roda antes de
cada implantação só existe em plano pago.

A imagem da api tinha outro obstáculo. Ela copiava a distribuição gerada fora do container, porque o módulo `shared`
tem alvo Android e o build dentro do container exigiria o SDK do Android.

## Decisão

O CI constrói as imagens da api e do importador em todo pull request e, na `main`, publica as duas no GitHub
Container Registry. A api roda no Render, no plano gratuito, a partir da imagem publicada, e o banco fica no Neon. O
importador não vai para o ar: a api sobe sem ele e responde 503 só na importação.

| Assunto | Escolha |
|---|---|
| Imagens | `ghcr.io/guilhermechaves0/fluxo/api` e `.../importador`, com a etiqueta do commit e `latest` |
| Build da api | Dois estágios. O Gradle roda dentro do container com `-Pfluxo.soApi=true`, que deixa de fora os módulos do app e o alvo Android de `shared` |
| Plataforma | Render, descrito no `render.yaml` da raiz, na região de Ohio, a mesma do banco |
| Banco | PostgreSQL gerenciado no Neon |
| Segredos | `DB_URL`, `DB_USER` e `DB_PASSWORD` ficam nos segredos do GitHub Actions e no painel do Render, nunca no repositório |
| Migrações | O job `migrar` do CI roda `api migrar` com a imagem recém-publicada contra o Neon. A api também migra ao subir |
| Nova versão no ar | O CI pede a implantação ao Render por um gancho, quando o segredo `RENDER_DEPLOY_HOOK` existe. Sem ele, a nova versão entra pelo painel |
| Saúde | `GET /health`, usado pelo Docker Compose e pelo Render |
| Serviço Go | Fica fora da implantação. No `render.yaml` ele está num bloco comentado, com o que falta para ligá-lo |

## Alternativas consideradas

| Alternativa | Por que descartei |
|---|---|
| Construir a imagem no próprio Render, a partir do Dockerfile | O que iria para o ar não seria o artefato que o CI construiu e conferiu. Com a imagem publicada, dá para voltar a um commit exato |
| Aplicar as migrações só na subida da api | A rubrica pede task ou CI, e uma migração que falha derrubaria a subida inteira. Com o job, a falha aparece antes, no log do CI |
| Usar o comando de pré-implantação do Render para migrar | Só existe em plano pago |
| Pôr o importador no ar | No plano gratuito do Render a api não o alcançaria por gRPC. Exigiria plano pago ou outra plataforma, e a rubrica trata o serviço Go no ar como opcional |
| Imagem de build com o SDK do Android | Centenas de megabytes e a aceitação de licenças para compilar um módulo que a api não usa |
| Banco no mesmo container da api | Não é banco gerenciado, e os dados se perderiam a cada implantação |

## Consequências

A cada commit na `main` existe uma imagem com a etiqueta dele, e `docker compose up --build` sobe tudo num clone
limpo, sem Gradle nem SDK na máquina.

Em compensação:

- Em produção a importação de extrato responde 503, porque o importador não está no ar. O resto da api funciona.
- A lista de dependências de `shared` existe em dois arquivos, `build.gradle.kts` e `build-api.gradle.kts`. Uma
  dependência nova precisa entrar nos dois.
- Os segredos do banco ficam em dois lugares, no GitHub e no Render, e trocar a senha exige atualizar os dois.
- As imagens só são publicadas depois que os testes passam, então um pull request leva mais tempo para ficar verde.
- A imagem da api tem cerca de 560 MB, por causa do JRE.

## Como verificar

- Os jobs `Imagens` e `Migrar o banco` do workflow de CI, com os logs de cada execução na `main`.
- `docker compose up --build --wait` num clone limpo e `docker compose ps` com os três serviços saudáveis.
- `docker compose run --rm fluxo-api migrar`, que termina com código 0 e o log do Flyway.
- `render.yaml`, sem nenhum valor de segredo.
