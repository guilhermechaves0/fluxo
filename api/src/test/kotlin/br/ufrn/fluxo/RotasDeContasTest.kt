package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.web.ContaDto
import br.ufrn.fluxo.adaptadores.web.PaginaDeContasDto
import br.ufrn.fluxo.adaptadores.web.Problema
import br.ufrn.fluxo.dominio.Conta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * As rotas de contas com a porta em memória: o que é HTTP (status, cabeçalhos e problem details) sem
 * banco e sem Docker. O SQL e as restrições do banco ficam para o IntegracaoPostgresTest.
 */
class RotasDeContasTest {
    private fun ApplicationTestBuilder.app(vararg contas: Conta): HttpClient {
        application { configurar(modulosEmMemoria(contas = ContasEmMemoria(*contas))) }
        return createClient { install(ContentNegotiation) { json() } }
    }

    private suspend fun HttpClient.enviar(metodo: HttpMethod, caminho: String, corpo: String) = request(caminho) {
        method = metodo
        contentType(ContentType.Application.Json)
        setBody(corpo)
    }

    /** Confere o tipo de mídia da RFC 9457 e devolve o corpo do erro. */
    private suspend fun HttpResponse.problema(): Problema {
        assertEquals(ContentType.Application.ProblemJson, contentType()?.withoutParameters())
        return Json.decodeFromString<Problema>(bodyAsText())
    }

    @Test
    fun criarDevolve201ComLocationEAContaFicaDisponivel() = testApplication {
        val cliente = app()
        val resposta = cliente.enviar(HttpMethod.Post, "/contas", """{"nome":"Nubank"}""")
        assertEquals(HttpStatusCode.Created, resposta.status)
        val criada = resposta.body<ContaDto>()
        assertEquals("Nubank", criada.nome)
        assertEquals("/contas/${criada.id}", resposta.headers[HttpHeaders.Location])
        assertEquals(criada, cliente.get("/contas/${criada.id}").body<ContaDto>())
    }

    @Test
    fun nomeVazioDevolve422EmProblemDetailsComAsViolacoes() = testApplication {
        val resposta = app().enviar(HttpMethod.Post, "/contas", """{"nome":"   "}""")
        assertEquals(HttpStatusCode.UnprocessableEntity, resposta.status)
        val problema = resposta.problema()
        assertEquals("https://fluxo.ufrn.br/erros/entrada-invalida", problema.type)
        assertEquals(listOf("o nome da conta é obrigatório"), problema.violacoes)
        assertEquals("/contas", problema.instance)
    }

    @Test
    fun corpoMalformadoOuSemNomeDevolve400() = testApplication {
        val cliente = app()
        assertEquals(HttpStatusCode.BadRequest, cliente.enviar(HttpMethod.Post, "/contas", """{"nome":""").status)
        val semNome = cliente.enviar(HttpMethod.Post, "/contas", "{}")
        assertEquals(HttpStatusCode.BadRequest, semNome.status)
        assertEquals("https://fluxo.ufrn.br/erros/requisicao-malformada", semNome.problema().type)
    }

    @Test
    fun nomeRepetidoDevolve409() = testApplication {
        val resposta = app(NUBANK).enviar(HttpMethod.Post, "/contas", """{"nome":"nubank"}""")
        assertEquals(HttpStatusCode.Conflict, resposta.status)
        assertEquals("https://fluxo.ufrn.br/erros/conflito", resposta.problema().type)
    }

    @Test
    fun contaInexistenteDevolve404NasTresOperacoes() = testApplication {
        val cliente = app()
        assertEquals(HttpStatusCode.NotFound, cliente.get("/contas/nao-existe").status)
        val renomeacao = cliente.enviar(HttpMethod.Put, "/contas/nao-existe", """{"nome":"Outra"}""")
        assertEquals(HttpStatusCode.NotFound, renomeacao.status)
        val remocao = cliente.delete("/contas/nao-existe")
        assertEquals(HttpStatusCode.NotFound, remocao.status)
        assertEquals("conta nao-existe não existe", remocao.problema().detail)
    }

    @Test
    fun renomearDevolveAContaComONomeNovo() = testApplication {
        val resposta = app(NUBANK).enviar(HttpMethod.Put, "/contas/c-1", """{"nome":"Nubank conta"}""")
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertEquals(ContaDto("c-1", "Nubank conta"), resposta.body<ContaDto>())
    }

    @Test
    fun removerDevolve204EAContaDeixaDeExistir() = testApplication {
        val cliente = app(NUBANK)
        assertEquals(HttpStatusCode.NoContent, cliente.delete("/contas/c-1").status)
        assertEquals(HttpStatusCode.NotFound, cliente.get("/contas/c-1").status)
    }

    @Test
    fun listagemPaginaEFiltraPeloNome() = testApplication {
        val cliente = app(NUBANK, CARTEIRA, Conta("c-3", "Banco do Brasil"))
        val primeira = cliente.get("/contas?tamanho=2").body<PaginaDeContasDto>()
        assertEquals(listOf("Banco do Brasil", "Carteira"), primeira.itens.map { it.nome })
        assertEquals(3, primeira.total)
        val segunda = cliente.get("/contas?tamanho=2&pagina=1").body<PaginaDeContasDto>()
        assertEquals(listOf("Nubank"), segunda.itens.map { it.nome })
        val filtrada = cliente.get("/contas?nome=BANK").body<PaginaDeContasDto>()
        assertEquals(listOf("Nubank"), filtrada.itens.map { it.nome })
    }

    @Test
    fun paginaComTipoErradoDevolve400EForaDaFaixaDevolve422() = testApplication {
        val cliente = app()
        val tipoErrado = cliente.get("/contas?pagina=abc")
        assertEquals(HttpStatusCode.BadRequest, tipoErrado.status)
        assertEquals("https://fluxo.ufrn.br/erros/requisicao-malformada", tipoErrado.problema().type)
        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.get("/contas?tamanho=101").status)
        assertEquals(HttpStatusCode.UnprocessableEntity, cliente.get("/contas?pagina=-1").status)
    }

    private companion object {
        val NUBANK = Conta("c-1", "Nubank")
        val CARTEIRA = Conta("c-2", "Carteira")
    }
}
