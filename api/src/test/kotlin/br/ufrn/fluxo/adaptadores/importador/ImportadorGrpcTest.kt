package br.ufrn.fluxo.adaptadores.importador

import br.ufrn.fluxo.aplicacao.ArquivoGrandeDemais
import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.importador.v1.Formato
import br.ufrn.fluxo.importador.v1.ImportadorServiceGrpcKt.ImportadorServiceCoroutineImplBase
import br.ufrn.fluxo.importador.v1.ImportarRequest
import br.ufrn.fluxo.importador.v1.ImportarResponse
import br.ufrn.fluxo.importador.v1.importarResponse
import br.ufrn.fluxo.importador.v1.lancamento
import br.ufrn.fluxo.importador.v1.linhaIgnorada
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import br.ufrn.fluxo.importador.v1.Tipo as TipoDoContrato

/**
 * O adaptador contra um servidor gRPC em memória (`InProcessServerBuilder`): a chamada passa pela pilha
 * gRPC inteira, com serialização, prazo e código de status, sem abrir porta nem subir o importador em Go.
 */
class ImportadorGrpcTest {
    private val fechar = mutableListOf<AutoCloseable>()

    @AfterTest
    fun encerrar() = fechar.reversed().forEach { it.close() }

    private fun importadorContra(
        servidor: ImportadorServiceCoroutineImplBase,
        prazo: Duration = 5.seconds,
    ): ImportadorGrpc {
        val nome = UUID.randomUUID().toString()
        val emMemoria = InProcessServerBuilder.forName(nome).addService(servidor).build().start()
        val adaptador = ImportadorGrpc(InProcessChannelBuilder.forName(nome).build(), prazo)
        fechar += AutoCloseable { emMemoria.shutdownNow() }
        fechar += adaptador
        return adaptador
    }

    private fun respondendo(resposta: suspend (ImportarRequest) -> ImportarResponse) =
        object : ImportadorServiceCoroutineImplBase() {
            override suspend fun importar(request: ImportarRequest) = resposta(request)
        }

    private fun recusando(status: Status) = respondendo { throw status.asException() }

    @Test
    fun converteARespostaDoImportador() = runTest {
        val importador = importadorContra(
            respondendo { pedido ->
                assertEquals("x", pedido.conteudo.toStringUtf8())
                importarResponse {
                    formato = Formato.FORMATO_CSV
                    origem = "C6 Bank (fatura do cartão)"
                    lancamentos += lancamento {
                        idExterno = "ab12"
                        data = "2026-09-05"
                        valorCentavos = 9_990
                        descricao = "LOJA EXEMPLO (2/3)"
                        tipo = TipoDoContrato.TIPO_DESPESA
                    }
                    lancamentos += lancamento {
                        idExterno = "cd34"
                        data = "2026-09-06"
                        valorCentavos = 150_000
                        descricao = "SALÁRIO"
                        tipo = TipoDoContrato.TIPO_RECEITA
                        categoria = "Renda"
                    }
                    linhasIgnoradas += linhaIgnorada {
                        posicao = 4
                        motivo = "linha de saldo"
                    }
                    ignorados = 1
                }
            },
        )

        val extrato = importador.ler("x".toByteArray())

        assertEquals("CSV", extrato.formato)
        assertEquals("C6 Bank (fatura do cartão)", extrato.origem)
        val (despesa, receita) = extrato.lancamentos
        assertEquals(9_990, despesa.valorCentavos)
        assertEquals(Tipo.DESPESA, despesa.tipo)
        assertEquals(LocalDate(2026, 9, 5), despesa.data)
        assertNull(despesa.categoria, "categoria vazia no contrato vira nula")
        assertEquals(Tipo.RECEITA, receita.tipo)
        assertEquals("Renda", receita.categoria)
        assertEquals("linha de saldo", extrato.ignorados.single().motivo)
        assertEquals(4, extrato.ignorados.single().posicao)
    }

    @Test
    fun formatoPdfChegaComoTexto() = runTest {
        val importador = importadorContra(respondendo { importarResponse { formato = Formato.FORMATO_PDF } })
        assertEquals("PDF", importador.ler("x".toByteArray()).formato)
    }

    @Test
    fun invalidArgumentViraExtratoInvalidoComODetalhe() = runTest {
        val detalhe = "formato de extrato não reconhecido"
        val importador = importadorContra(recusando(Status.INVALID_ARGUMENT.withDescription(detalhe)))
        val erro = assertFailsWith<ExtratoInvalido> { importador.ler("x".toByteArray()) }
        assertEquals(detalhe, erro.message)
    }

    @Test
    fun resourceExhaustedViraArquivoGrandeDemais() = runTest {
        val importador = importadorContra(recusando(Status.RESOURCE_EXHAUSTED))
        assertFailsWith<ArquivoGrandeDemais> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun unavailableViraImportadorIndisponivel() = runTest {
        val importador = importadorContra(recusando(Status.UNAVAILABLE))
        assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun deadlineExceededDoServidorViraImportadorIndisponivel() = runTest {
        val importador = importadorContra(recusando(Status.DEADLINE_EXCEEDED))
        assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun erroInternoDoImportadorViraImportadorIndisponivel() = runTest {
        val importador = importadorContra(recusando(Status.INTERNAL.withDescription("erro interno ao ler o extrato")))
        assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun servidorSemOServicoViraImportadorIndisponivel() = runTest {
        // Sem override, o servidor responde UNIMPLEMENTED.
        val importador = importadorContra(object : ImportadorServiceCoroutineImplBase() {})
        assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun respostaForaDoContratoViraImportadorIndisponivel() = runTest {
        val importador = importadorContra(respondendo { importarResponse { origem = "sem formato" } })
        assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
    }

    @Test
    fun importadorForaDoArViraImportadorIndisponivel() = runTest {
        val canal: ManagedChannel = InProcessChannelBuilder.forName("nao-existe-" + UUID.randomUUID()).build()
        val importador = ImportadorGrpc(canal, prazo = 5.seconds)
        fechar += importador
        val erro = assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
        assertEquals(Status.Code.UNAVAILABLE, Status.fromThrowable(erro.cause).code)
    }

    @Test
    fun servidorMaisLentoQueOPrazoResponde503DentroDoPrazo() = runTest {
        val prazo = 300.milliseconds
        val importador = importadorContra(
            respondendo {
                delay(10.seconds)
                importarResponse { formato = Formato.FORMATO_OFX }
            },
            prazo = prazo,
        )

        val inicio = System.nanoTime()
        val erro = assertFailsWith<ImportadorIndisponivel> { importador.ler("x".toByteArray()) }
        val gasto = (System.nanoTime() - inicio).nanoseconds

        assertEquals(Status.Code.DEADLINE_EXCEEDED, Status.fromThrowable(erro.cause).code)
        assertTrue(gasto < prazo + 2.seconds, "o prazo era $prazo e a chamada levou $gasto")
    }
}
