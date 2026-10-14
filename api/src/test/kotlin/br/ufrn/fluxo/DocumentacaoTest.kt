package br.ufrn.fluxo

import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * O OpenAPI gerado das rotas e a Swagger UI, com as portas em memória: confere que o documento descreve
 * todas as rotas, os erros de cada uma e os tipos dos parâmetros e dos corpos, e que a interface responde.
 */
class DocumentacaoTest {
    private fun ApplicationTestBuilder.app() = application { configurar(modulosEmMemoria()) }

    private suspend fun ApplicationTestBuilder.documento(): JsonObject {
        app()
        val resposta = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertEquals(ContentType.Application.Json, resposta.contentType()?.withoutParameters())
        return Json.parseToJsonElement(resposta.bodyAsText()).jsonObject
    }

    private fun JsonObject.obj(vararg chaves: String): JsonObject =
        chaves.fold(this) { atual, chave -> atual.getValue(chave).jsonObject }

    private fun JsonObject.texto(chave: String) = getValue(chave).jsonPrimitive.content

    private fun JsonObject.caminhos() = obj("paths")

    /** `GET /contas` vira o objeto da operação `get` do caminho `/contas`. */
    private fun JsonObject.operacao(nome: String): JsonObject {
        val (metodo, caminho) = nome.split(" ")
        return obj("paths", caminho, metodo.lowercase())
    }

    private fun JsonObject.parametro(nome: String): JsonObject =
        getValue("parameters").jsonArray.map { it.jsonObject }.first { it.texto("name") == nome }

    private fun JsonObject.tipoDoSchema() = obj("schema").texto("type")

    private fun JsonObject.referencia() = texto("\$ref")

    /** Todos os `$ref` do documento, em qualquer nível. */
    private fun JsonElement.referencias(): List<String> = when (this) {
        is JsonObject -> flatMap { (chave, valor) -> referenciasDe(chave, valor) }
        is JsonArray -> flatMap { it.referencias() }
        else -> emptyList()
    }

    private fun referenciasDe(chave: String, valor: JsonElement) =
        if (chave == "\$ref") listOf(valor.jsonPrimitive.content) else valor.referencias()

    /** O primeiro exemplo do conteúdo `application/json`, no objeto `conteudo` de um corpo ou de uma resposta. */
    private fun JsonObject.primeiroExemplo(): JsonElement =
        obj("application/json", "examples").values.first().jsonObject.getValue("value")

    private suspend fun ApplicationTestBuilder.enviar(metodo: HttpMethod, caminho: String, corpo: String) =
        client.request(caminho) {
            method = metodo
            contentType(ContentType.Application.Json)
            setBody(corpo)
        }

    @Test
    fun openApiListaOsCaminhosDaApiEEscondeAsRotasDeDocumentacao() = testApplication {
        val caminhos = documento().caminhos().keys
        assertEquals(
            setOf(
                "/health",
                "/importacoes/previa",
                "/contas",
                "/contas/{id}",
                "/contas/{id}/transacoes",
                "/contas/{id}/transacoes/{transacaoId}",
            ),
            caminhos,
        )
    }

    @Test
    fun openApiDescreveUmaOperacaoPorMetodoHttp() = testApplication {
        val operacoes =
            documento().caminhos().flatMap { (caminho, item) ->
                item.jsonObject.keys.map { "${it.uppercase()} $caminho" }
            }
        assertEquals(OPERACOES.toList().sorted(), operacoes.sorted())
    }

    @Test
    fun cadaRotaDocumentaSeusErrosEmProblemJsonComOSchemaProblema() = testApplication {
        val documento = documento()
        assertTrue("application/problem+json" in documento.toString())
        ERROS_POR_OPERACAO.forEach { (nome, esperados) ->
            val respostas = documento.operacao(nome).obj("responses")
            val erros = respostas.keys.map { it.toInt() }.filter { it >= HttpStatusCode.BadRequest.value }
            assertEquals(esperados.sorted(), erros.sorted(), "erros documentados de $nome")
            erros.forEach { status ->
                val problema = respostas.obj("$status", "content", "application/problem+json", "schema")
                assertEquals("#/components/schemas/Problema", problema.referencia(), "schema do $status de $nome")
            }
        }
    }

    @Test
    fun paginaETamanhoSaoInteirosComOsLimitesDaPaginacao() = testApplication {
        val documento = documento()
        listOf("GET /contas", "GET /contas/{id}/transacoes").forEach { nome ->
            val operacao = documento.operacao(nome)
            val pagina = operacao.parametro("pagina")
            assertEquals("query", pagina.texto("in"))
            assertEquals("integer", pagina.tipoDoSchema(), "tipo de pagina em $nome")
            assertEquals(0.0, pagina.obj("schema").getValue("minimum").jsonPrimitive.double)
            val tamanho = operacao.parametro("tamanho")
            assertEquals("integer", tamanho.tipoDoSchema(), "tipo de tamanho em $nome")
            val limites = tamanho.obj("schema")
            assertEquals(1.0, limites.getValue("minimum").jsonPrimitive.double)
            assertEquals(100.0, limites.getValue("maximum").jsonPrimitive.double)
            assertEquals(20, limites.getValue("default").jsonPrimitive.int)
        }
    }

    @Test
    fun filtrosEIdsDeCaminhoTemOTipoCerto() = testApplication {
        val documento = documento()
        val nome = documento.operacao("GET /contas").parametro("nome")
        assertEquals("string", nome.tipoDoSchema())

        val listagem = documento.operacao("GET /contas/{id}/transacoes")
        val tipo = listagem.parametro("tipo").obj("schema")
        assertEquals(listOf("RECEITA", "DESPESA"), tipo.getValue("enum").jsonArray.map { it.jsonPrimitive.content })
        val mes = listagem.parametro("mes")
        assertEquals("string", mes.tipoDoSchema())
        val formato = Regex(mes.obj("schema").texto("pattern"))
        assertTrue(formato.matches("2026-09"))
        listOf("2026-00", "2026-13", "2026-9", "26-09").forEach { assertFalse(formato.matches(it), "aceitou $it") }
        assertEquals("string", listagem.parametro("categoria").tipoDoSchema())
        assertEquals("string", listagem.parametro("descricao").tipoDoSchema())

        val remocao = documento.operacao("DELETE /contas/{id}/transacoes/{transacaoId}")
        listOf("id", "transacaoId").forEach { id ->
            val parametro = remocao.parametro(id)
            assertEquals("path", parametro.texto("in"))
            assertTrue(parametro.getValue("required").jsonPrimitive.boolean)
            assertEquals("string", parametro.tipoDoSchema())
        }
    }

    @Test
    fun corposERespostasDeSucessoUsamOsDtosDaApi() = testApplication {
        val documento = documento()
        SCHEMAS_DE_ENTRADA.forEach { (nome, dto) ->
            val corpo = documento.operacao(nome).obj("requestBody", "content", "application/json", "schema")
            assertEquals("#/components/schemas/$dto", corpo.referencia(), "corpo de $nome")
        }
        SCHEMAS_DE_SAIDA.forEach { (nome, saida) ->
            val (status, dto) = saida
            val resposta = documento.operacao(nome).obj("responses", status, "content", "application/json", "schema")
            assertEquals("#/components/schemas/$dto", resposta.referencia(), "resposta $status de $nome")
        }
        listOf("POST /contas", "POST /contas/{id}/transacoes").forEach { nome ->
            val location = documento.operacao(nome).obj("responses", "201", "headers", "Location")
            assertEquals("string", location.tipoDoSchema(), "cabeçalho Location de $nome")
        }
    }

    @Test
    fun todaReferenciaDoDocumentoApontaParaUmSchemaDefinido() = testApplication {
        val documento = documento()
        val definidos = documento.obj("components", "schemas").keys.map { "#/components/schemas/$it" }.toSet()
        val usados = documento.referencias().toSet()
        assertTrue(usados.isNotEmpty())
        assertTrue(definidos.containsAll(usados), "referências sem schema: ${usados - definidos}")
    }

    @Test
    fun osExemplosDosCorposSaoAceitosPelaPropriaApi() = testApplication {
        val documento = documento()
        fun exemplo(operacao: String) =
            documento.operacao(operacao).obj("requestBody", "content").primeiroExemplo().toString()

        val conta = enviar(HttpMethod.Post, "/contas", exemplo("POST /contas"))
        assertEquals(HttpStatusCode.Created, conta.status, conta.bodyAsText())
        val contaId = Json.parseToJsonElement(conta.bodyAsText()).jsonObject.texto("id")
        val renomeada = enviar(HttpMethod.Put, "/contas/$contaId", exemplo("PUT /contas/{id}"))
        assertEquals(HttpStatusCode.OK, renomeada.status, renomeada.bodyAsText())

        val transacoes = "/contas/$contaId/transacoes"
        val lancada = enviar(HttpMethod.Post, transacoes, exemplo("POST /contas/{id}/transacoes"))
        assertEquals(HttpStatusCode.Created, lancada.status, lancada.bodyAsText())
        val transacaoId = Json.parseToJsonElement(lancada.bodyAsText()).jsonObject.texto("id")
        val atualizacao = exemplo("PUT /contas/{id}/transacoes/{transacaoId}")
        val atualizada = enviar(HttpMethod.Put, "$transacoes/$transacaoId", atualizacao)
        assertEquals(HttpStatusCode.OK, atualizada.status, atualizada.bodyAsText())
    }

    @Test
    fun oExemploDoHealthEIgualAoQueARotaDevolve() = testApplication {
        val documento = documento()
        val exemplo = documento.operacao("GET /health").obj("responses", "200", "content").primeiroExemplo()
        val resposta = client.get("/health")
        assertEquals(exemplo, Json.parseToJsonElement(resposta.bodyAsText()))
    }

    @Test
    fun previaDescreveOArquivoEmMultipartEDiretoNoCorpo() = testApplication {
        val corpo = documento().operacao("POST /importacoes/previa").obj("requestBody")
        assertTrue(corpo.getValue("required").jsonPrimitive.boolean)
        val multipart = corpo.obj("content", "multipart/form-data", "schema")
        assertEquals(listOf("arquivo"), multipart.getValue("required").jsonArray.map { it.jsonPrimitive.content })
        assertEquals("binary", multipart.obj("properties", "arquivo").texto("format"))
        val direto = corpo.obj("content", "application/octet-stream", "schema")
        assertEquals("binary", direto.texto("format"))
    }

    @Test
    fun swaggerUiRespondeEApontaParaODocumentoDasRotas() = testApplication {
        app()
        val pagina = client.get("/swagger")
        assertEquals(HttpStatusCode.OK, pagina.status)
        assertEquals(ContentType.Text.Html, pagina.contentType()?.withoutParameters())
        val aponta = Regex("""url: '([^']+)'""").find(pagina.bodyAsText())
        val endereco = assertNotNull(aponta, "a página não aponta o documento").groupValues[1]
        val documento = client.get(endereco)
        assertEquals(HttpStatusCode.OK, documento.status)
        assertTrue("\"/contas/{id}/transacoes/{transacaoId}\"" in documento.bodyAsText())
    }

    private companion object {
        val OPERACOES =
            setOf(
                "GET /health",
                "POST /importacoes/previa",
                "GET /contas",
                "POST /contas",
                "GET /contas/{id}",
                "PUT /contas/{id}",
                "DELETE /contas/{id}",
                "GET /contas/{id}/transacoes",
                "POST /contas/{id}/transacoes",
                "GET /contas/{id}/transacoes/{transacaoId}",
                "PUT /contas/{id}/transacoes/{transacaoId}",
                "DELETE /contas/{id}/transacoes/{transacaoId}",
            )

        /** Os status de erro que cada rota declara, conforme o `StatusPages` e os casos de uso. */
        val ERROS_POR_OPERACAO =
            mapOf(
                "GET /health" to emptyList<Int>(),
                "POST /importacoes/previa" to listOf(400, 413, 422, 503),
                "GET /contas" to listOf(400, 422),
                "POST /contas" to listOf(400, 409, 415, 422),
                "GET /contas/{id}" to listOf(400, 404),
                "PUT /contas/{id}" to listOf(400, 404, 409, 415, 422),
                "DELETE /contas/{id}" to listOf(400, 404),
                "GET /contas/{id}/transacoes" to listOf(400, 404, 422),
                "POST /contas/{id}/transacoes" to listOf(400, 404, 415, 422),
                "GET /contas/{id}/transacoes/{transacaoId}" to listOf(400, 404),
                "PUT /contas/{id}/transacoes/{transacaoId}" to listOf(400, 404, 415, 422),
                "DELETE /contas/{id}/transacoes/{transacaoId}" to listOf(400, 404),
            )

        val SCHEMAS_DE_ENTRADA =
            mapOf(
                "POST /contas" to "NovaContaDto",
                "PUT /contas/{id}" to "NovaContaDto",
                "POST /contas/{id}/transacoes" to "NovaTransacaoDto",
                "PUT /contas/{id}/transacoes/{transacaoId}" to "NovaTransacaoDto",
            )

        /** O status de sucesso de cada rota e o DTO que ele devolve. */
        val SCHEMAS_DE_SAIDA =
            mapOf(
                "POST /importacoes/previa" to ("200" to "PreviaDto"),
                "GET /contas" to ("200" to "PaginaDeContasDto"),
                "POST /contas" to ("201" to "ContaDto"),
                "GET /contas/{id}" to ("200" to "ContaDto"),
                "PUT /contas/{id}" to ("200" to "ContaDto"),
                "GET /contas/{id}/transacoes" to ("200" to "PaginaDeTransacoesDto"),
                "POST /contas/{id}/transacoes" to ("201" to "TransacaoDto"),
                "GET /contas/{id}/transacoes/{transacaoId}" to ("200" to "TransacaoDto"),
                "PUT /contas/{id}/transacoes/{transacaoId}" to ("200" to "TransacaoDto"),
            )
    }
}
