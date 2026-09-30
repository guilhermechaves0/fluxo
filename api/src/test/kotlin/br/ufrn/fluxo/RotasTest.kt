package br.ufrn.fluxo

import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ExtratoLido
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
import br.ufrn.fluxo.aplicacao.LancamentoLido
import br.ufrn.fluxo.aplicacao.LinhaIgnorada
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotasTest {
    @Test
    fun healthRespondeUp() = testApplication {
        application { configurar(modulosEmMemoria()) }
        val resposta = client.get("/health")
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(resposta.bodyAsText().contains("UP"))
    }

    @Test
    fun transacoesVemDasMaisRecentesParaAsMaisAntigas() = testApplication {
        application { configurar(modulosEmMemoria()) }
        val resposta = client.get("/transacoes")
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status)
        assertTrue(corpo.indexOf("t-03") in 0 until corpo.indexOf("t-01"), corpo)
    }

    @Test
    fun previaRecebeOArquivoEmMultipartEDevolveAsTransacoes() = testApplication {
        comImportador { EXTRATO }
        val resposta =
            client.submitFormWithBinaryData(
                "/importacoes/previa",
                formData {
                    append(
                        "arquivo",
                        "conteudo do ofx".toByteArray(),
                        Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"extrato.ofx\"") },
                    )
                },
            )
        val corpo = resposta.bodyAsText()
        assertEquals(HttpStatusCode.OK, resposta.status, corpo)
        assertTrue(corpo.contains("\"origem\":\"Nubank (conta)\""), corpo)
        assertTrue(corpo.contains("\"id\":\"imp-nubank-fit-1\""), corpo)
        assertTrue(corpo.contains("\"motivo\":\"data inválida\""), corpo)
    }

    @Test
    fun previaAceitaOArquivoDiretoNoCorpo() = testApplication {
        comImportador { EXTRATO }
        val resposta = client.post("/importacoes/previa") { setBody("conteudo".toByteArray()) }
        assertEquals(HttpStatusCode.OK, resposta.status)
    }

    @Test
    fun extratoNaoReconhecidoVira422() = testApplication {
        comImportador { throw ExtratoInvalido("o cabeçalho \"a, b\" não tem colunas que eu reconheça") }
        val resposta = client.post("/importacoes/previa") { setBody("a;b".toByteArray()) }
        assertEquals(HttpStatusCode.UnprocessableEntity, resposta.status)
        assertTrue(resposta.bodyAsText().contains("extrato-nao-reconhecido"))
    }

    @Test
    fun importadorForaDoArVira503() = testApplication {
        comImportador { throw ImportadorIndisponivel() }
        val resposta = client.post("/importacoes/previa") { setBody("x".toByteArray()) }
        assertEquals(HttpStatusCode.ServiceUnavailable, resposta.status)
        assertTrue(resposta.bodyAsText().contains("importador-indisponivel"))
    }

    @Test
    fun arquivoVazioVira422() = testApplication {
        comImportador { EXTRATO }
        val resposta = client.post("/importacoes/previa") { setBody(ByteArray(0)) }
        assertEquals(HttpStatusCode.UnprocessableEntity, resposta.status)
    }

    private fun ApplicationTestBuilder.comImportador(ler: () -> ExtratoLido) {
        val falso =
            object : ImportadorDeExtratos {
                override suspend fun ler(conteudo: ByteArray) = ler()
            }
        application { configurar(modulosEmMemoria(importador = falso)) }
    }

    private companion object {
        val EXTRATO =
            ExtratoLido(
                formato = "OFX",
                origem = "Nubank (conta)",
                lancamentos =
                listOf(LancamentoLido("fit-1", LocalDate(2026, 9, 6), 4_590, "PADARIA EXEMPLO", Tipo.DESPESA)),
                ignorados = listOf(LinhaIgnorada(3, "data inválida")),
            )
    }
}
