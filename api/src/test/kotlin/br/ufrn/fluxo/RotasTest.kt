package br.ufrn.fluxo

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotasTest {
    @Test
    fun healthRespondeUp() = testApplication {
        application { modulo(URL_IMPORTADOR) }
        val resposta = client.get("/health")
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(resposta.bodyAsText().contains("UP"))
    }

    @Test
    fun transacoesVemDasMaisRecentesParaAsMaisAntigas() = testApplication {
        application { modulo(URL_IMPORTADOR) }
        val resposta = client.get("/transacoes")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(corpo.indexOf("t-03") in 0 until corpo.indexOf("t-01"), corpo)
    }

    @Test
    fun importacaoAindaNaoImplementadaRespondeProblemDetails() = testApplication {
        application { modulo(URL_IMPORTADOR) }
        val resposta = client.post("/importacoes")
        assertEquals(HttpStatusCode.NotImplemented, resposta.status)
        assertTrue(resposta.bodyAsText().contains("nao-implementado"))
    }

    private companion object {
        const val URL_IMPORTADOR = "http://localhost:9090"
    }
}
