package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DA_CATEGORIA
import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DA_DESCRICAO
import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DO_NOME_DA_CONTA
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.ExampleObject
import io.ktor.openapi.GenericElement
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.OpenApiInfo
import io.ktor.openapi.Operation
import io.ktor.openapi.Parameters
import io.ktor.openapi.ReferenceOr
import io.ktor.openapi.Responses
import io.ktor.openapi.jsonSchema

/** Título, versão e descrição que abrem o documento OpenAPI e a Swagger UI. */
internal val INFO_DA_API = OpenApiInfo(
    title = "API do Fluxo",
    version = "0.2.0",
    description = "Contas e transações do assistente de finanças pessoais Fluxo, com a prévia da importação de " +
        "extratos OFX e CSV. Toda resposta de erro volta no formato application/problem+json, da RFC 9457, " +
        "inclusive escape de porcentagem inválido no endereço (400), rota inexistente (404), método que a rota " +
        "não aceita (405 nas rotas sem parâmetro no caminho; nas rotas com `{id}` o roteador responde 404) e " +
        "`Accept` sem JSON (406).",
)

private typealias Descricao = Operation.Builder.() -> Unit

/**
 * A documentação OpenAPI das rotas, fora da árvore de rotas.
 *
 * Cada propriedade é o bloco que o `.describe(...)` de uma rota recebe. O Ktor monta o OpenAPI a partir desses
 * blocos e dos tipos dos DTOs, então não existe arquivo estático que possa divergir das rotas: ao alterar uma
 * rota, altera-se o bloco dela aqui. As respostas de erro seguem o `StatusPages` (Erros.kt) e saem em
 * `application/problem+json`, com o corpo [Problema].
 */
object Doc {
    val health: Descricao = {
        summary = "Verificação de saúde"
        description = "Responde 200 enquanto a api está no ar."
        tag("operação")
        responses {
            HttpStatusCode.OK {
                description = "A api está no ar"
                ContentType.Application.Json {
                    schema = jsonSchema<Map<String, String>>()
                    example("up", EXEMPLO_DE_HEALTH)
                }
            }
        }
    }

    // ---------------------------------------------------------------- importação

    val previaDeImportacao: Descricao = {
        summary = "Prévia da importação de um extrato"
        description =
            "Lê o extrato OFX ou CSV pelo importador em Go e devolve as transações que ele geraria, sem gravar " +
            "nada. O app mostra a prévia e decide o que importar."
        tag("importação")
        requestBody {
            required = true
            description = "O extrato, com até 5 MB, no campo `arquivo` de um multipart/form-data ou direto no corpo."
            ContentType.MultiPart.FormData {
                schema = JsonSchema(
                    type = JsonType.OBJECT,
                    properties = mapOf("arquivo" to ReferenceOr.value(ARQUIVO)),
                    required = listOf("arquivo"),
                )
            }
            ContentType.Application.OctetStream { schema = ARQUIVO }
        }
        responses {
            HttpStatusCode.OK {
                description = "As transações que o extrato geraria e as linhas que o importador descartou"
                schema = jsonSchema<PreviaDto>()
            }
            problema(
                HttpStatusCode.BadRequest,
                "Content-Type ou corpo multipart/form-data malformado, por exemplo sem o `boundary`",
            )
            problema(HttpStatusCode.PayloadTooLarge, "O extrato passa de 5 MB")
            problema(HttpStatusCode.UnprocessableEntity, "Arquivo vazio ou extrato que o importador não reconheceu")
            problema(HttpStatusCode.ServiceUnavailable, "O importador de extratos não respondeu")
        }
    }

    // ---------------------------------------------------------------- contas

    val listarContas: Descricao = {
        summary = "Lista as contas"
        description = "Paginada e em ordem alfabética do nome."
        tag("contas")
        parameters {
            paginacao()
            query("nome") {
                description = "Parte do nome da conta, sem diferenciar maiúsculas de minúsculas."
                schema = jsonSchema<String>()
            }
        }
        responses {
            HttpStatusCode.OK {
                description = "Uma página de contas"
                schema = jsonSchema<PaginaDeContasDto>()
            }
            problema(
                HttpStatusCode.BadRequest,
                "`pagina` ou `tamanho` que não é um número inteiro, ou `nome` com o caractere nulo",
            )
            problema(HttpStatusCode.UnprocessableEntity, FORA_DA_FAIXA)
        }
    }

    val criarConta: Descricao = {
        summary = "Cria uma conta"
        description =
            "O `id` é atribuído pela api e o endereço da conta criada volta no cabeçalho `Location`. " +
            "Duas contas não têm o mesmo nome, sem diferenciar maiúsculas de minúsculas."
        tag("contas")
        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<NovaContaDto>()
                example("conta", EXEMPLO_DE_CONTA)
            }
        }
        responses {
            HttpStatusCode.Created {
                description = "Conta criada"
                schema = jsonSchema<ContaDto>()
                headers {
                    header(HttpHeaders.Location) {
                        description = "Endereço da conta criada, como `/contas/{id}`"
                        schema = jsonSchema<String>()
                    }
                }
            }
            escritaDeConta()
        }
    }

    val buscarConta: Descricao = {
        summary = "Busca uma conta pelo id"
        tag("contas")
        parameters { idDaConta() }
        responses {
            HttpStatusCode.OK {
                description = "A conta"
                schema = jsonSchema<ContaDto>()
            }
            problema(HttpStatusCode.BadRequest, ID_COM_NULO)
            problema(HttpStatusCode.NotFound, CONTA_INEXISTENTE)
        }
    }

    val renomearConta: Descricao = {
        summary = "Renomeia uma conta"
        description = "O PUT troca o nome da conta. O `id` não muda."
        tag("contas")
        parameters { idDaConta() }
        requestBody {
            required = true
            ContentType.Application.Json {
                schema = jsonSchema<NovaContaDto>()
                example("conta", EXEMPLO_DE_CONTA)
            }
        }
        responses {
            HttpStatusCode.OK {
                description = "A conta com o nome novo"
                schema = jsonSchema<ContaDto>()
            }
            escritaDeConta(naoEncontrado = CONTA_INEXISTENTE)
        }
    }

    val removerConta: Descricao = {
        summary = "Remove uma conta e as transações dela"
        tag("contas")
        parameters { idDaConta() }
        responses {
            HttpStatusCode.NoContent { description = "Conta removida" }
            problema(HttpStatusCode.BadRequest, ID_COM_NULO)
            problema(HttpStatusCode.NotFound, CONTA_INEXISTENTE)
        }
    }

    // ---------------------------------------------------------------- transações

    val listarTransacoes: Descricao = {
        summary = "Lista as transações de uma conta"
        description = "Da data mais recente para a mais antiga. Os filtros se combinam com E."
        tag("transações")
        parameters {
            idDaConta()
            paginacao()
            query("tipo") {
                description = "Só as receitas ou só as despesas."
                schema = jsonSchema<String>().copy(enum = Tipo.entries.map { GenericElement(it.name) })
            }
            query("mes") {
                description = "Só as transações com data neste mês, no formato aaaa-mm, como 2026-09."
                schema = jsonSchema<String>().copy(pattern = FORMATO_DO_MES)
            }
            query("categoria") {
                description = "Id da categoria, como `mercado`."
                schema = jsonSchema<String>()
            }
            query("descricao") {
                description = "Parte da descrição, sem diferenciar maiúsculas de minúsculas."
                schema = jsonSchema<String>()
            }
        }
        responses {
            HttpStatusCode.OK {
                description = "Uma página de transações"
                schema = jsonSchema<PaginaDeTransacoesDto>()
            }
            problema(
                HttpStatusCode.BadRequest,
                "`pagina` ou `tamanho` que não é um número inteiro, `tipo` diferente de RECEITA e " +
                    "DESPESA, `mes` fora do formato aaaa-mm, ou `id` ou um filtro com o caractere nulo",
            )
            problema(HttpStatusCode.NotFound, CONTA_INEXISTENTE)
            problema(HttpStatusCode.UnprocessableEntity, FORA_DA_FAIXA)
        }
    }

    val criarTransacao: Descricao = {
        summary = "Cria uma transação na conta"
        description = "O `id` é atribuído pela api e o endereço da transação criada volta no cabeçalho `Location`."
        tag("transações")
        parameters { idDaConta() }
        requestBody {
            required = true
            description = CORPO_DA_TRANSACAO
            ContentType.Application.Json {
                schema = jsonSchema<NovaTransacaoDto>()
                example("despesa", EXEMPLO_DE_TRANSACAO)
            }
        }
        responses {
            HttpStatusCode.Created {
                description = "Transação criada"
                schema = jsonSchema<TransacaoDto>()
                headers {
                    header(HttpHeaders.Location) {
                        description = "Endereço da transação criada, como `/contas/{id}/transacoes/{transacaoId}`"
                        schema = jsonSchema<String>()
                    }
                }
            }
            escritaDeTransacao(naoEncontrado = CONTA_INEXISTENTE)
        }
    }

    val buscarTransacao: Descricao = {
        summary = "Busca uma transação da conta"
        tag("transações")
        parameters {
            idDaConta()
            idDaTransacao()
        }
        responses {
            HttpStatusCode.OK {
                description = "A transação"
                schema = jsonSchema<TransacaoDto>()
            }
            problema(HttpStatusCode.BadRequest, ID_COM_NULO)
            problema(HttpStatusCode.NotFound, CONTA_OU_TRANSACAO_INEXISTENTE)
        }
    }

    val atualizarTransacao: Descricao = {
        summary = "Substitui uma transação da conta"
        description = "O PUT troca todos os campos da transação. O `id` não muda."
        tag("transações")
        parameters {
            idDaConta()
            idDaTransacao()
        }
        requestBody {
            required = true
            description = CORPO_DA_TRANSACAO
            ContentType.Application.Json {
                schema = jsonSchema<NovaTransacaoDto>()
                example("despesa", EXEMPLO_DE_TRANSACAO)
            }
        }
        responses {
            HttpStatusCode.OK {
                description = "A transação com os dados novos"
                schema = jsonSchema<TransacaoDto>()
            }
            escritaDeTransacao(naoEncontrado = CONTA_OU_TRANSACAO_INEXISTENTE)
        }
    }

    val removerTransacao: Descricao = {
        summary = "Remove uma transação da conta"
        tag("transações")
        parameters {
            idDaConta()
            idDaTransacao()
        }
        responses {
            HttpStatusCode.NoContent { description = "Transação removida" }
            problema(HttpStatusCode.BadRequest, ID_COM_NULO)
            problema(HttpStatusCode.NotFound, CONTA_OU_TRANSACAO_INEXISTENTE)
        }
    }
}

// ------------------------------------------------------------------ peças comuns

private const val CONTA_INEXISTENTE = "Conta inexistente"
private const val CONTA_OU_TRANSACAO_INEXISTENTE = "Conta ou transação inexistente"
private const val ID_COM_NULO = "Id do caminho com o caractere nulo"
private const val CORPO_NAO_JSON = "Corpo enviado em outro formato que não application/json"
private const val FORA_DA_FAIXA =
    "`pagina` negativa ou `tamanho` fora de 1 a ${PedidoDePagina.TAMANHO_MAXIMO}; os motivos vêm em `violacoes`"
private const val FORMATO_DO_MES = "^\\d{4}-(0[1-9]|1[0-2])$"
private const val CORPO_DA_TRANSACAO =
    "`valorCentavos` é positivo e o sinal vem do `tipo`. `data` vai como aaaa-mm-dd, `tipo` é RECEITA ou " +
        "DESPESA e `categoria`, que é opcional, é o nome, como Mercado."

// Exemplos para a Swagger UI: o "Try it out" já sai com dados que a api aceita, e o DocumentacaoTest manda cada
// corpo de exemplo para a própria api.

private val EXEMPLO_DE_HEALTH = ExampleObject(
    summary = "A api no ar",
    value = GenericElement(mapOf("status" to "UP")),
)

private val EXEMPLO_DE_CONTA = ExampleObject(
    summary = "Uma conta corrente",
    value = GenericElement(NovaContaDto(nome = "Nubank")),
)

private val EXEMPLO_DE_TRANSACAO = ExampleObject(
    summary = "Uma despesa no mercado",
    value = GenericElement(
        NovaTransacaoDto(
            descricao = "Supermercado",
            valorCentavos = 31_245,
            data = "2026-09-06",
            tipo = Tipo.DESPESA.name,
            categoria = "Mercado",
        ),
    ),
)

/** O arquivo do extrato, como o OpenAPI descreve um campo de upload. */
private val ARQUIVO = JsonSchema(type = JsonType.STRING, format = "binary", description = "O extrato OFX ou CSV")

private fun Parameters.Builder.idDaConta() = path("id") {
    description = "Id da conta, atribuído pela api."
    schema = jsonSchema<String>()
}

private fun Parameters.Builder.idDaTransacao() = path("transacaoId") {
    description = "Id da transação, atribuído pela api."
    schema = jsonSchema<String>()
}

private fun Parameters.Builder.paginacao() {
    query("pagina") {
        description = "Página, a partir de 0. Padrão: 0."
        schema = jsonSchema<Int>().copy(minimum = 0.0, default = GenericElement(0))
    }
    query("tamanho") {
        description =
            "Itens por página, de 1 a ${PedidoDePagina.TAMANHO_MAXIMO}. Padrão: ${PedidoDePagina.TAMANHO_PADRAO}."
        schema = jsonSchema<Int>().copy(
            minimum = 1.0,
            maximum = PedidoDePagina.TAMANHO_MAXIMO.toDouble(),
            default = GenericElement(PedidoDePagina.TAMANHO_PADRAO),
        )
    }
}

private fun Responses.Builder.problema(status: HttpStatusCode, descricao: String) = status {
    description = descricao
    ContentType.Application.ProblemJson { schema = jsonSchema<Problema>() }
}

/**
 * As respostas de erro do POST e do PUT de conta, na ordem dos status. O PUT passa o [naoEncontrado], e o 404
 * entra entre o 400 e o 409.
 */
private fun Responses.Builder.escritaDeConta(naoEncontrado: String? = null) {
    problema(
        HttpStatusCode.BadRequest,
        "JSON malformado, `nome` ausente ou de tipo errado, ou `nome` ou `id` com o caractere nulo",
    )
    naoEncontrado?.let { problema(HttpStatusCode.NotFound, it) }
    problema(HttpStatusCode.Conflict, "Já existe outra conta com esse nome, sem diferenciar maiúsculas de minúsculas")
    problema(HttpStatusCode.UnsupportedMediaType, CORPO_NAO_JSON)
    problema(
        HttpStatusCode.UnprocessableEntity,
        "Nome vazio ou com mais de $TAMANHO_MAXIMO_DO_NOME_DA_CONTA caracteres; os motivos vêm em `violacoes`",
    )
}

/** As respostas de erro do POST e do PUT de transação, na ordem dos status, com o 404 de cada um. */
private fun Responses.Builder.escritaDeTransacao(naoEncontrado: String) {
    problema(
        HttpStatusCode.BadRequest,
        "JSON malformado, campo obrigatório ausente ou de tipo errado, `data` fora do formato " +
            "aaaa-mm-dd ou com o ano fora de 1 a 9999, `tipo` diferente de RECEITA e DESPESA, ou texto ou id " +
            "com o caractere nulo",
    )
    problema(HttpStatusCode.NotFound, naoEncontrado)
    problema(HttpStatusCode.UnsupportedMediaType, CORPO_NAO_JSON)
    problema(
        HttpStatusCode.UnprocessableEntity,
        "Descrição vazia ou com mais de $TAMANHO_MAXIMO_DA_DESCRICAO caracteres, valor menor ou igual a zero ou " +
            "categoria com mais de $TAMANHO_MAXIMO_DA_CATEGORIA caracteres; os motivos vêm em `violacoes`",
    )
}
