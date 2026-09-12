package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.ListarTransacoes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

/**
 * Rotas HTTP da api. Cada rota só converte a requisição em chamada de caso de uso; as regras de
 * negócio ficam em aplicacao/ e no domínio.
 */
fun Application.rotas(urlImportador: String) {
    val listarTransacoes by inject<ListarTransacoes>()

    routing {
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        get("/transacoes") {
            call.respond(listarTransacoes().map { it.paraDto() })
        }

        // Na Sprint 1 esta rota recebe o arquivo do extrato e o repassa ao importador em Go.
        post("/importacoes") {
            call.respond(
                HttpStatusCode.NotImplemented,
                Problema(
                    type = "https://fluxo.ufrn.br/erros/nao-implementado",
                    title = "Importação de extrato",
                    status = HttpStatusCode.NotImplemented.value,
                    detail = "Entra na Sprint 1, encaminhando o arquivo para $urlImportador/importar.",
                ),
            )
        }
    }
}
