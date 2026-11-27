package br.ufrn.fluxo.adaptadores.importador

import br.ufrn.fluxo.canalDoImportador
import br.ufrn.fluxo.importador.v1.Formato
import br.ufrn.fluxo.importador.v1.ImportadorServiceGrpcKt.ImportadorServiceCoroutineImplBase
import br.ufrn.fluxo.importador.v1.ImportarRequest
import br.ufrn.fluxo.importador.v1.importarResponse
import br.ufrn.fluxo.importador.v1.lancamento
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val TAMANHO_DA_DESCRICAO = 5 * 1024 * 1024

/**
 * O canal de produção contra um servidor gRPC de rede (porta livre). O transporte em memória não aplica o
 * limite de mensagem recebida, por isso este teste precisa de um servidor de verdade.
 */
class CanalDoImportadorTest {
    private val fechar = mutableListOf<AutoCloseable>()

    @AfterTest
    fun encerrar() = fechar.reversed().forEach { it.close() }

    @Test
    fun aceitaRespostaAcimaDosQuatroMegabytesPadraoDoGrpc() = runTest {
        val servidor = NettyServerBuilder.forPort(0)
            .addService(
                object : ImportadorServiceCoroutineImplBase() {
                    override suspend fun importar(request: ImportarRequest) = importarResponse {
                        formato = Formato.FORMATO_CSV
                        lancamentos += lancamento {
                            data = "2026-09-05"
                            descricao = "x".repeat(TAMANHO_DA_DESCRICAO)
                            tipo = br.ufrn.fluxo.importador.v1.Tipo.TIPO_DESPESA
                        }
                    }
                },
            ).build().start()
        fechar += AutoCloseable { servidor.shutdownNow() }
        val importador = ImportadorGrpc(canalDoImportador("localhost:${servidor.port}"))
        fechar += importador

        val extrato = importador.ler("x".toByteArray())

        assertEquals(TAMANHO_DA_DESCRICAO, extrato.lancamentos.single().descricao.length)
    }
}
