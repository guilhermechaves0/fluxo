package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.CadastroDeContas
import br.ufrn.fluxo.aplicacao.FiltroDeContas
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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
 * CRUD de contas.
 *
 * | Método | Caminho      | Sucesso                  |
 * |--------|--------------|--------------------------|
 * | GET    | /contas      | 200, paginado e filtrado |
 * | POST   | /contas      | 201 com Location         |
 * | GET    | /contas/{id} | 200                      |
 * | PUT    | /contas/{id} | 200                      |
 * | DELETE | /contas/{id} | 204                      |
 *
 * Remover uma conta remove as transações dela. As rotas das transações ficam em RotasDeTransacoes.kt.
 * Os erros (400, 404, 409 e 422) saem do StatusPages, em Erros.kt, e não daqui.
 */
fun Route.rotasDeContas() {
    val cadastro by inject<CadastroDeContas>()

    route("/contas") {
        get {
            val filtro = FiltroDeContas(nome = call.texto("nome"))
            call.respond(cadastro.listar(filtro, call.pedidoDePagina()).paraDto())
        }

        post {
            val criada = cadastro.criar(call.receive<NovaContaDto>().nome)
            call.response.header(HttpHeaders.Location, "/contas/${criada.id}")
            call.respond(HttpStatusCode.Created, criada.paraDto())
        }

        route("/{id}") {
            get {
                call.respond(cadastro.buscar(call.caminho("id")).paraDto())
            }

            put {
                val nome = call.receive<NovaContaDto>().nome
                call.respond(cadastro.renomear(call.caminho("id"), nome).paraDto())
            }

            delete {
                cadastro.remover(call.caminho("id"))
                call.respond(HttpStatusCode.NoContent)
            }

            rotasDeTransacoes()
        }
    }
}
