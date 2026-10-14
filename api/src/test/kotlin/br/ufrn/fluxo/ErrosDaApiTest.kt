package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.web.Problema
import br.ufrn.fluxo.dominio.Conta
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLDecodeException
import io.ktor.http.contentType
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Os erros que não vêm de uma regra do domínio: a rota, o método e o formato de resposta que a api não tem, e a
 * entrada que o PostgreSQL recusaria. Todos saem em problem details (RFC 9457), sem resposta vazia e sem 500.
 * Roda com as portas em memória, sem banco.
 */
class ErrosDaApiTest {
    private fun ApplicationTestBuilder.app(): HttpClient {
        application { configurar(modulosEmMemoria(contas = ContasEmMemoria(Conta("c-1", "Nubank")))) }
        return client
    }

    private suspend fun HttpClient.enviar(metodo: HttpMethod, caminho: String, corpo: String) = request(caminho) {
        method = metodo
        contentType(ContentType.Application.Json)
        setBody(corpo)
    }

    private suspend fun HttpClient.lancar(corpo: String) = enviar(HttpMethod.Post, TRANSACOES, corpo)

    private fun corpoDaTransacao(descricao: String = "Padaria", dia: String = "2026-09-06", categoria: String? = null) =
        """{"descricao":"$descricao","valorCentavos":4590,"data":"$dia","tipo":"DESPESA"""" +
            categoria?.let { ""","categoria":"$it"""" }.orEmpty() + "}"

    /** Confere o tipo de mídia da RFC 9457 e devolve o corpo do erro. */
    private suspend fun HttpResponse.problema(): Problema {
        assertEquals(ContentType.Application.ProblemJson, contentType()?.withoutParameters())
        return Json.decodeFromString<Problema>(bodyAsText())
    }

    /** Confere o status, o `type` e o `detail` do erro e devolve o problema para outras conferências. */
    private suspend fun HttpResponse.confere(status: HttpStatusCode, tipo: String, detalhe: String): Problema {
        assertEquals(status, this.status, bodyAsText())
        val problema = problema()
        assertEquals("https://fluxo.ufrn.br/erros/$tipo", problema.type)
        assertEquals(status.value, problema.status)
        assertEquals(detalhe, problema.detail)
        return problema
    }

    private suspend fun HttpResponse.confereMalformada(detalhe: String) =
        confere(HttpStatusCode.BadRequest, "requisicao-malformada", detalhe)

    private suspend fun HttpResponse.confereAnoForaDaFaixa(dia: String) =
        confereMalformada("data deve ter o ano entre 1 e 9999, e veio \"$dia\"")

    // ---------------------------------------------------------------- 404, 405 e 406 que o Ktor devolve sem corpo

    @Test
    fun rotaInexistenteDevolve404EmProblemDetails() = testApplication {
        val cliente = app()
        val problema =
            cliente.get("/nao-existe").confere(
                HttpStatusCode.NotFound,
                "rota-inexistente",
                "não existe rota para GET /nao-existe",
            )
        assertEquals("Rota inexistente", problema.title)
        assertEquals("/nao-existe", problema.instance)
        // Dentro de um caminho que existe, e com outro método.
        cliente.delete("/contas/c-1/outra").confere(
            HttpStatusCode.NotFound,
            "rota-inexistente",
            "não existe rota para DELETE /contas/c-1/outra",
        )
        // O Accept de quem pede texto não troca o 404 por um 406.
        cliente.get("/nao-existe") { header(HttpHeaders.Accept, "text/plain") }.confere(
            HttpStatusCode.NotFound,
            "rota-inexistente",
            "não existe rota para GET /nao-existe",
        )
    }

    @Test
    fun recursoInexistenteContinuaComOProblemaNaoEncontrado() = testApplication {
        val cliente = app()
        cliente.get("/contas/nao-existe").confere(
            HttpStatusCode.NotFound,
            "nao-encontrado",
            "conta nao-existe não existe",
        )
        cliente.get("$TRANSACOES/t-9").confere(HttpStatusCode.NotFound, "nao-encontrado", "transação t-9 não existe")
    }

    @Test
    fun metodoNaoPermitidoDevolve405EmProblemDetails() = testApplication {
        val cliente = app()
        val problema =
            cliente.request("/contas") { method = HttpMethod.Patch }.confere(
                HttpStatusCode.MethodNotAllowed,
                "metodo-nao-permitido",
                "PATCH não é permitido em /contas",
            )
        assertEquals("Método não permitido", problema.title)
        assertEquals("/contas", problema.instance)
        cliente.get("/importacoes/previa").confere(
            HttpStatusCode.MethodNotAllowed,
            "metodo-nao-permitido",
            "GET não é permitido em /importacoes/previa",
        )
    }

    @Test
    fun acceptSemJsonDevolve406EmProblemDetails() = testApplication {
        val cliente = app()
        val problema =
            cliente.get("/contas") { header(HttpHeaders.Accept, "text/plain") }.confere(
                HttpStatusCode.NotAcceptable,
                "formato-nao-aceito",
                "a api responde em application/json",
            )
        assertEquals("Formato de resposta não aceito", problema.title)
        assertEquals("/contas", problema.instance)
        cliente.get("/health") { header(HttpHeaders.Accept, "application/xml") }.confere(
            HttpStatusCode.NotAcceptable,
            "formato-nao-aceito",
            "a api responde em application/json",
        )
    }

    @Test
    fun acceptComJsonOuQualquerFormatoContinuaDevolvendo200() = testApplication {
        val cliente = app()
        assertEquals(HttpStatusCode.OK, cliente.get("/contas").status)
        listOf("application/json", "*/*", "text/plain, application/json;q=0.5", "application/*").forEach { aceito ->
            val resposta = cliente.get("/contas") { header(HttpHeaders.Accept, aceito) }
            assertEquals(HttpStatusCode.OK, resposta.status, "Accept: $aceito")
            assertEquals(ContentType.Application.Json, resposta.contentType()?.withoutParameters(), "Accept: $aceito")
        }
    }

    @Test
    fun headNaoLevaCorpoNemNaRotaInexistenteNemNoMetodoNaoPermitido() = testApplication {
        val cliente = app()
        val esperados = mapOf("/nao-existe" to HttpStatusCode.NotFound, "/contas" to HttpStatusCode.MethodNotAllowed)
        esperados.forEach { (caminho, status) ->
            val resposta = cliente.request(caminho) { method = HttpMethod.Head }
            assertEquals(status, resposta.status, caminho)
            assertEquals(null, resposta.contentType(), caminho)
            assertEquals("", resposta.bodyAsText(), caminho)
        }
    }

    // ---------------------------------------------------------------- escape de porcentagem inválido na consulta

    @Test
    fun escapeDePorcentagemInvalidoDevolve400EmVezDe500() = testApplication {
        val mensagem = "Wrong HEX escape: %ZZ, in %ZZ, at 0"
        application {
            configurar(modulosEmMemoria())
            // O cliente de teste não monta um endereço com %ZZ. A rota lança o que o Ktor lança ao ler a consulta.
            routing { route("/consulta-invalida") { handle { throw URLDecodeException(mensagem) } } }
        }
        client.get("/consulta-invalida").confere(HttpStatusCode.BadRequest, "requisicao-malformada", mensagem)
    }

    // ---------------------------------------------------------------- caractere nulo, que o PostgreSQL recusa

    @Test
    fun caractereNuloNoCaminhoDevolve400() = testApplication {
        val cliente = app()
        cliente.get("/contas/%00").confereMalformada(NULO_NO_ID)
        cliente.delete("/contas/%00").confereMalformada(NULO_NO_ID)
        cliente.get("/contas/%00/transacoes").confereMalformada(NULO_NO_ID)
        cliente.get("$TRANSACOES/%00").confereMalformada("transacaoId não pode conter o caractere nulo")
    }

    @Test
    fun caractereNuloNosFiltrosDevolve400() = testApplication {
        val cliente = app()
        cliente.get("/contas?nome=%00").confereMalformada("nome não pode conter o caractere nulo")
        cliente.get("/contas?nome=Nu%00bank").confereMalformada("nome não pode conter o caractere nulo")
        cliente.get("$TRANSACOES?descricao=%00").confereMalformada("descricao não pode conter o caractere nulo")
        cliente.get("$TRANSACOES?categoria=%00").confereMalformada("categoria não pode conter o caractere nulo")
    }

    @Test
    fun caractereNuloNoCorpoDevolve400() = testApplication {
        val cliente = app()
        // Num texto bruto o \u0000 fica como está, e é o JSON que o transforma no caractere nulo.
        cliente.enviar(HttpMethod.Post, "/contas", """{"nome":"Nu\u0000bank"}""")
            .confereMalformada("nome não pode conter o caractere nulo")
        cliente.enviar(HttpMethod.Put, "/contas/c-1", """{"nome":"\u0000"}""")
            .confereMalformada("nome não pode conter o caractere nulo")
        cliente.lancar(corpoDaTransacao(descricao = """Pa\u0000daria"""))
            .confereMalformada("descricao não pode conter o caractere nulo")
        cliente.lancar(corpoDaTransacao(categoria = """\u0000"""))
            .confereMalformada("categoria não pode conter o caractere nulo")
        cliente.enviar(HttpMethod.Put, "$TRANSACOES/t-1", corpoDaTransacao(descricao = """\u0000"""))
            .confereMalformada("descricao não pode conter o caractere nulo")
    }

    // ---------------------------------------------------------------- ano fora do que o PostgreSQL guarda

    @Test
    fun anoAcimaDe9999Devolve400() = testApplication {
        val cliente = app()
        listOf("+5874898-01-01", "+10000-01-01").forEach { dia ->
            cliente.lancar(corpoDaTransacao(dia = dia)).confereAnoForaDaFaixa(dia)
        }
        cliente.enviar(HttpMethod.Put, "$TRANSACOES/t-1", corpoDaTransacao(dia = "+10000-01-01"))
            .confereAnoForaDaFaixa("+10000-01-01")
    }

    @Test
    fun anoAbaixoDe1Devolve400() = testApplication {
        val cliente = app()
        listOf("0000-12-31", "-0001-01-01").forEach { dia ->
            cliente.lancar(corpoDaTransacao(dia = dia)).confereAnoForaDaFaixa(dia)
        }
    }

    @Test
    fun osAnosLimiteSaoAceitos() = testApplication {
        val cliente = app()
        listOf("0001-01-01", "9999-12-31").forEach { dia ->
            val resposta = cliente.lancar(corpoDaTransacao(dia = dia))
            assertEquals(HttpStatusCode.Created, resposta.status, resposta.bodyAsText())
        }
    }

    private companion object {
        const val TRANSACOES = "/contas/c-1/transacoes"
        const val NULO_NO_ID = "id não pode conter o caractere nulo"
    }
}
