package br.ufrn.fluxo.adaptadores.importador

import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
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

class ImportadorHttpTest {
    private fun importador(status: HttpStatusCode, corpo: String, tipo: String = "application/json") = ImportadorHttp(
        url = "http://importador",
        cliente =
        HttpClient(
            MockEngine { requisicao ->
                assertEquals("http://importador/importar", requisicao.url.toString())
                respond(corpo, status, headersOf(HttpHeaders.ContentType, tipo))
            },
        ) { install(ContentNegotiation) { json() } },
    )

    @Test
    fun converteARespostaDoImportador() = runTest {
        val corpo =
            """{"formato":"CSV","origem":"C6 Bank (fatura do cartão)","lancamentos":[{"idExterno":"ab12",""" +
                """"data":"2026-09-05","valorCentavos":9990,"descricao":"LOJA EXEMPLO (2/3)","tipo":"DESPESA"}],""" +
                """"ignorados":[{"posicao":4,"motivo":"linha de saldo"}]}"""
        val extrato = importador(HttpStatusCode.OK, corpo).ler("x".toByteArray())
        assertEquals("C6 Bank (fatura do cartão)", extrato.origem)
        assertEquals(9_990, extrato.lancamentos.single().valorCentavos)
        assertEquals(Tipo.DESPESA, extrato.lancamentos.single().tipo)
        assertEquals("linha de saldo", extrato.ignorados.single().motivo)
    }

    @Test
    fun recusaDoImportadorViraExtratoInvalidoComODetalhe() = runTest {
        val corpo = """{"type":"x","status":422,"detail":"formato de extrato não reconhecido"}"""
        val erro =
            assertFailsWith<ExtratoInvalido> {
                importador(HttpStatusCode.UnprocessableEntity, corpo, "application/problem+json").ler("x".toByteArray())
            }
        assertEquals("formato de extrato não reconhecido", erro.message)
    }

    @Test
    fun falhaDeConexaoViraImportadorIndisponivel() = runTest {
        val foraDoAr =
            ImportadorHttp("http://importador", HttpClient(MockEngine { throw IOException("conexão recusada") }))
        assertFailsWith<ImportadorIndisponivel> { foraDoAr.ler("x".toByteArray()) }
    }

    @Test
    fun erroInternoDoImportadorViraImportadorIndisponivel() = runTest {
        assertFailsWith<ImportadorIndisponivel> {
            importador(HttpStatusCode.InternalServerError, "{}").ler("x".toByteArray())
        }
    }
}
