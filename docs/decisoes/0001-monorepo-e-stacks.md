# ADR-0001: Um monorepo para duas disciplinas, com Kotlin (KMP e Ktor) e Go

**Estado:** Aceita
**Data:** 2026-09-12

## Contexto

O mesmo produto atende a DIM0524, que avalia o aplicativo em Kotlin Multiplatform, e a DIM0547, que avalia um serviço
principal em Kotlin com Ktor ou em Java com Quarkus, microsserviços em Go e contratos gRPC. Faço o projeto sozinho, e as
entregas das duas disciplinas vencem nas mesmas datas. O projeto de referência das disciplinas, o MUSI, usa um monorepo
em que o domínio fica num módulo `shared/`, importado pela api e pelo aplicativo.

A partir da versão 9 do Android Gradle Plugin (AGP), o plugin Kotlin Multiplatform não pode ser aplicado no mesmo módulo
que o `com.android.application`. O AGP 9 também exige JDK 17 ou mais recente.

## Decisão

Uso um único repositório público, dividido nestes módulos:

| Módulo | Conteúdo |
|---|---|
| `shared/` | Domínio, em Kotlin Multiplatform com alvos JVM e Android |
| `app/` | Interface em Compose Multiplatform, com alvo desktop e como biblioteca Android |
| `app-android/` | Aplicativo Android que gera o APK a partir de `app/` |
| `api/` | Serviço principal com Ktor e Koin |
| `services/` | Importador de extratos em Go, só com a biblioteca padrão |
| `protos/` | Contrato gRPC entre a api e o importador |

Fixei as versões das ferramentas no `mise.toml`, com o JDK 21.

## Alternativas consideradas

| Alternativa | Por que descartei |
|---|---|
| Um repositório por disciplina | O domínio ficaria duplicado, e a integração entre as disciplinas, mais frágil |
| Um só módulo `app/` com `com.android.application` e Kotlin Multiplatform | O AGP 9 não permite essa combinação |
| `shared/` só com alvo JVM | O `app/` tem alvo Android, e o template oficial de Kotlin Multiplatform declara o mesmo alvo nos módulos de que o app depende |
| JDK 25, como no MUSI | Não testei essa versão com o AGP neste projeto, e o MUSI ainda não tem alvo Android |
| Java com Quarkus no serviço principal | Eu teria de reescrever em Java o domínio que o aplicativo compartilha em Kotlin (seção 5.1 da proposta) |

## Consequências

Escrevo e testo o domínio uma única vez, e as duas disciplinas usam o mesmo pipeline e o mesmo quadro de tarefas.

Em compensação, configurar a `api` exige o SDK do Android, porque o `shared/` tem alvo Android. Por isso a imagem Docker
da api copia a distribuição que o Gradle gera fora do container, em vez de compilar dentro dele. O aplicativo também
passou a ocupar dois módulos, `app/` e `app-android/`, em vez de um.

## Como verificar

O `mise run ci` e o job `ci` do GitHub Actions terminam sem erro na `main`, e `mise x -- java -version` mostra o JDK 21.
