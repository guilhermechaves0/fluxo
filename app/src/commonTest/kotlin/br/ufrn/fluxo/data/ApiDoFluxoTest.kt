package br.ufrn.fluxo.data

import br.ufrn.fluxo.dominio.Tipo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApiDoFluxoTest {
    private fun api(status: HttpStatusCode, corpo: String, tipo: String = "application/json") = ApiDoFluxo(
        url = "http://api",
        cliente =
        HttpClient(
            MockEngine { requisicao ->
                assertEquals("http://api/importacoes/previa", requisicao.url.toString())
                respond(corpo, status, headersOf(HttpHeaders.ContentType, tipo))
            },
        ) { install(ContentNegotiation) { json() } },
    )

    @Test
    fun previaViraTransacoesDoDominio() = runTest {
        val corpo =
            """{"formato":"OFX","origem":"Nubank","transacoes":[{"id":"imp-nubank-1","descricao":"PADARIA",""" +
                """"valorCentavos":4590,"data":"2026-09-06","tipo":"DESPESA","categoria":"Restaurantes"}],""" +
                """"ignorados":[{"posicao":3,"motivo":"x"}]}"""
        val extrato = api(HttpStatusCode.OK, corpo).previaDeImportacao("extrato.ofx", byteArrayOf(1))
        assertEquals("Nubank", extrato.origem)
        assertEquals(Tipo.DESPESA, extrato.transacoes.single().tipo)
        assertEquals("restaurantes", extrato.transacoes.single().categoria?.id)
        assertEquals(listOf("x"), extrato.ignorados)
    }

    @Test
    fun erroDaApiLevaODetalheParaATela() = runTest {
        val corpo = """{"type":"x","title":"Extrato não reconhecido","status":422,"detail":"cabeçalho sem data"}"""
        val falha =
            assertFailsWith<FalhaNaApi> {
                api(HttpStatusCode.UnprocessableEntity, corpo, "application/problem+json")
                    .previaDeImportacao("a.csv", byteArrayOf(1))
            }
        assertEquals("cabeçalho sem data", falha.message)
    }

    @Test
    fun apiForaDoArDizOndeProcurou() = runTest {
        val foraDoAr = ApiDoFluxo("http://api", HttpClient(MockEngine { throw IOException("recusada") }))
        val falha = assertFailsWith<FalhaNaApi> { foraDoAr.previaDeImportacao("a.ofx", byteArrayOf(1)) }
        assertTrue(falha.message!!.contains("http://api"))
    }
}
