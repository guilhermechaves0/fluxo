package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.FiltroDeTransacoes
import br.ufrn.fluxo.aplicacao.TransacoesDaConta
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

/**
 * CRUD das transações de uma conta. Fica dentro de `/contas/{id}`, e a relação entre as duas
 * entidades aparece no endereço.
 *
 * | Método | Caminho                                  | Sucesso                  |
 * |--------|------------------------------------------|--------------------------|
 * | GET    | /contas/{id}/transacoes                  | 200, paginado e filtrado |
 * | POST   | /contas/{id}/transacoes                  | 201 com Location         |
 * | GET    | /contas/{id}/transacoes/{transacaoId}    | 200                      |
 * | PUT    | /contas/{id}/transacoes/{transacaoId}    | 200                      |
 * | DELETE | /contas/{id}/transacoes/{transacaoId}    | 204                      |
 *
 * Filtros da listagem: `tipo`, `mes` (aaaa-mm), `categoria` (o id, como `mercado`) e `descricao`.
 */
fun Route.rotasDeTransacoes() {
    val transacoes by inject<TransacoesDaConta>()

    route("/transacoes") {
        get {
            val pagina = transacoes.listar(call.caminho("id"), call.filtroDeTransacoes(), call.pedidoDePagina())
            call.respond(pagina.paraDto())
        }

        post {
            val contaId = call.caminho("id")
            val criada = transacoes.criar(contaId, call.receive<NovaTransacaoDto>().paraDados())
            call.response.header(HttpHeaders.Location, "/contas/$contaId/transacoes/${criada.id}")
            call.respond(HttpStatusCode.Created, criada.paraDto())
        }

        route("/{transacaoId}") {
            get {
                call.respond(transacoes.buscar(call.caminho("id"), call.caminho("transacaoId")).paraDto())
            }

            put {
                val dados = call.receive<NovaTransacaoDto>().paraDados()
                call.respond(transacoes.atualizar(call.caminho("id"), call.caminho("transacaoId"), dados).paraDto())
            }

            delete {
                transacoes.remover(call.caminho("id"), call.caminho("transacaoId"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.filtroDeTransacoes() = FiltroDeTransacoes(
    tipo = tipo("tipo"),
    mes = mes("mes"),
    categoria = texto("categoria"),
    descricao = texto("descricao"),
)
