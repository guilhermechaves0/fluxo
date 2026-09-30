package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.ListarTransacoes
import br.ufrn.fluxo.aplicacao.PreverImportacao
import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.koin.ktor.ext.inject

/**
 * Rotas HTTP da api. Cada rota só converte a requisição em chamada de caso de uso; as regras de
 * negócio ficam em aplicacao/ e no domínio.
 */
fun Application.rotas() {
    val listarTransacoes by inject<ListarTransacoes>()
    val preverImportacao by inject<PreverImportacao>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        get("/transacoes") {
            call.respond(listarTransacoes().map { it.paraDto() })
        }

        // Prévia: lê o extrato pelo importador em Go e devolve as transações, sem gravar. Aceita o
        // arquivo em multipart/form-data (campo "arquivo") ou direto no corpo.
        post("/importacoes/previa") {
            call.respond(preverImportacao(call.receberArquivo()).paraDto())
        }

        rotasDeContas()
    }
}

private const val LIMITE_DO_ARQUIVO = 5L * 1024 * 1024 + 1

/** Lê o arquivo enviado pelo app. Lê um byte além do limite para o caso de uso recusar o excesso. */
private suspend fun ApplicationCall.receberArquivo(): ByteArray {
    if (!request.contentType().match(ContentType.MultiPart.FormData)) return receive<ByteArray>()
    var conteudo = ByteArray(0)
    receiveMultipart(formFieldLimit = LIMITE_DO_ARQUIVO).forEachPart { parte ->
        if (parte is PartData.FileItem && parte.name == "arquivo") {
            conteudo = parte.provider().readRemaining().readByteArray()
        }
        parte.dispose()
    }
    return conteudo
}
