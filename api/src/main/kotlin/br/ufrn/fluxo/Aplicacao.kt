package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.web.Problema
import br.ufrn.fluxo.adaptadores.web.rotas
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin

private const val PORTA_PADRAO = 8080

fun main() {
    val porta = System.getenv("PORT")?.toIntOrNull() ?: PORTA_PADRAO
    val urlImportador = System.getenv("FLUXO_IMPORTADOR_URL") ?: "http://localhost:9090"

    // A engine CIO usa corrotinas e ocupa menos memória que a Netty.
    embeddedServer(CIO, port = porta, host = "0.0.0.0") {
        modulo(urlImportador)
    }.start(wait = true)
}

/** Instala os plugins do Ktor e registra as rotas. */
fun Application.modulo(urlImportador: String) {
    install(Koin) { modules(modulosDaAplicacao()) }

    install(ContentNegotiation) { json(Json { explicitNulls = false }) }

    install(CallLogging)

    // Exceções são convertidas em respostas no formato da RFC 9457 (application/problem+json).
    install(StatusPages) {
        exception<IllegalArgumentException> { call, causa ->
            call.respond(
                HttpStatusCode.UnprocessableEntity,
                Problema(
                    type = "https://fluxo.ufrn.br/erros/entrada-invalida",
                    title = "Entrada inválida",
                    status = HttpStatusCode.UnprocessableEntity.value,
                    detail = causa.message,
                ),
            )
        }
        exception<Throwable> { call, causa ->
            call.application.environment.log.error("erro não tratado", causa)
            call.respond(
                HttpStatusCode.InternalServerError,
                Problema(
                    type = "https://fluxo.ufrn.br/erros/interno",
                    title = "Erro interno",
                    status = HttpStatusCode.InternalServerError.value,
                ),
            )
        }
    }

    rotas(urlImportador)
}
