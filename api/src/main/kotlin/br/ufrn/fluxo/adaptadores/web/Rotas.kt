package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.ArquivoGrandeDemais
import br.ufrn.fluxo.aplicacao.PreverImportacao
import io.ktor.http.ContentType
import io.ktor.http.cio.ParserException
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.openapi.OpenApiDoc
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.openapi.hide
import io.ktor.server.routing.openapi.plus
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import io.ktor.utils.io.ExperimentalKtorApi
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.coroutineScope
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject

/**
 * Rotas HTTP da api. Cada rota só converte a requisição em chamada de caso de uso; as regras de
 * negócio ficam em aplicacao/ e no domínio.
 *
 * Cada rota termina em `.describe(Doc.x)`, e o OpenAPI é gerado desses blocos, que ficam em Documentacao.kt.
 * `describe` é API experimental no Ktor 3.5.
 */
@OptIn(ExperimentalKtorApi::class)
fun Application.rotas() {
    val preverImportacao by inject<PreverImportacao>()

    routing {
        rotasDeDocumentacao()

        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }.describe(Doc.health)

        // Prévia: lê o extrato pelo importador em Go e devolve as transações, sem gravar. Aceita o
        // arquivo em multipart/form-data (campo "arquivo") ou direto no corpo.
        post("/importacoes/previa") {
            call.respond(preverImportacao(call.receberArquivo()).paraDto())
        }.describe(Doc.previaDeImportacao)

        rotasDeContas()
    }
}

/** O documento OpenAPI gerado das rotas e a Swagger UI que o mostra. As duas ficam fora do próprio documento. */
@OptIn(ExperimentalKtorApi::class)
private fun Route.rotasDeDocumentacao() {
    get("/openapi.json") {
        call.respond(OpenApiDoc(info = INFO_DA_API) + call.application.routingRoot.descendants())
    }.hide()

    swaggerUI(path = "swagger") {
        info = INFO_DA_API
        source = OpenApiDocSource.Routing(contentType = ContentType.Application.Json)
    }.hide()
}

private const val LIMITE_DO_ARQUIVO = 5L * 1024 * 1024 + 1

/**
 * Lê o arquivo enviado pelo app. Lê um byte além do limite para o caso de uso recusar o excesso.
 *
 * O Ktor lê as partes do multipart numa corrotina própria, e uma falha dela derrubaria a chamada como 500. O
 * `coroutineScope` traz essa falha de volta para cá: corpo acima do limite vira 413, e corpo que não dá para ler
 * (sem `boundary`, com cabeçalho de parte quebrado) vira 400.
 */
private suspend fun ApplicationCall.receberArquivo(): ByteArray {
    if (!request.contentType().match(ContentType.MultiPart.FormData)) return receive<ByteArray>()
    var conteudo = ByteArray(0)
    val falha =
        runCatching {
            coroutineScope {
                receiveMultipart(formFieldLimit = LIMITE_DO_ARQUIVO).forEachPart { parte ->
                    if (parte is PartData.FileItem && parte.name == "arquivo") {
                        conteudo = parte.provider().readRemaining().readByteArray()
                    }
                    parte.release()
                }
            }
        }.exceptionOrNull()
    if (falha != null) throw erroDoMultipart(falha)
    return conteudo
}

/** Traduz a falha da leitura do multipart. O que não é erro de leitura, como o cancelamento, segue como veio. */
private fun ApplicationCall.erroDoMultipart(falha: Throwable): Throwable = when {
    falha !is IOException && falha !is ParserException && falha !is NumberFormatException -> falha
    (request.contentLength() ?: 0) > LIMITE_DO_ARQUIVO -> ArquivoGrandeDemais()
    else -> FormatoInvalido("o corpo multipart/form-data está malformado; envie o extrato no campo arquivo", falha)
}
