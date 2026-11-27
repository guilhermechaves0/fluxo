package br.ufrn.fluxo.adaptadores.importador

import br.ufrn.fluxo.aplicacao.ArquivoGrandeDemais
import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ExtratoLido
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
import br.ufrn.fluxo.aplicacao.LancamentoLido
import br.ufrn.fluxo.aplicacao.LinhaIgnorada
import br.ufrn.fluxo.importador.v1.ImportadorServiceGrpcKt.ImportadorServiceCoroutineStub
import br.ufrn.fluxo.importador.v1.ImportarResponse
import br.ufrn.fluxo.importador.v1.importarRequest
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.datetime.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import br.ufrn.fluxo.dominio.Tipo as TipoDoDominio
import br.ufrn.fluxo.importador.v1.Formato as FormatoDoContrato
import br.ufrn.fluxo.importador.v1.Tipo as TipoDoContrato

/** Prazo de cada chamada ao importador. Ver [ImportadorGrpc]. */
val PRAZO_PADRAO_DO_IMPORTADOR: Duration = 10.seconds

private val ESPERA_AO_FECHAR = 5.seconds

/**
 * Chama o ImportadorService do importador em Go por gRPC, com o stub de corrotinas gerado de protos/.
 *
 * Toda chamada leva prazo ([prazo]). Escolhi 10 segundos: o importador lê um extrato de até 5 MB em
 * menos de um segundo, e a fatura em PDF é a leitura mais lenta, em alguns segundos. O que passa disso
 * é serviço travado, e quem usa o app prefere um 503 rápido a esperar. O prazo também chega ao Go, que
 * para a leitura quando ele vence.
 *
 * O canal pertence a este adaptador: [close] o fecha quando a aplicação para.
 */
class ImportadorGrpc(private val canal: ManagedChannel, private val prazo: Duration = PRAZO_PADRAO_DO_IMPORTADOR) :
    ImportadorDeExtratos,
    AutoCloseable {
    private val stub = ImportadorServiceCoroutineStub(canal)

    override suspend fun ler(conteudo: ByteArray): ExtratoLido {
        val resposta = try {
            stub
                .withDeadlineAfter(prazo.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .importar(importarRequest { this.conteudo = ByteString.copyFrom(conteudo) })
        } catch (e: StatusException) {
            throw traduzir(e.status, e)
        } catch (e: StatusRuntimeException) {
            throw traduzir(e.status, e)
        }
        return resposta.paraDominio()
    }

    /** Espera as chamadas em andamento por um instante e corta o que sobrar. */
    override fun close() {
        canal.shutdown()
        if (!canal.awaitTermination(ESPERA_AO_FECHAR.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
            canal.shutdownNow()
        }
    }

    // O que o Go devolve, em termos da api: arquivo recusado (422 ou 413) ou importador fora de serviço (503).
    // Código que não conheço também vira 503, porque o erro é do importador e não do arquivo.
    private fun traduzir(status: Status, causa: Throwable): RuntimeException = when (status.code) {
        Status.Code.INVALID_ARGUMENT -> ExtratoInvalido(status.description ?: "O importador recusou o arquivo.")
        Status.Code.RESOURCE_EXHAUSTED -> ArquivoGrandeDemais()
        else -> ImportadorIndisponivel(causa) // UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL, UNIMPLEMENTED...
    }
}

private fun ImportarResponse.paraDominio(): ExtratoLido = try {
    ExtratoLido(
        formato = formato.paraTexto(),
        origem = origem,
        lancamentos = lancamentosList.map {
            LancamentoLido(
                idExterno = it.idExterno,
                data = LocalDate.parse(it.data),
                valorCentavos = it.valorCentavos,
                descricao = it.descricao,
                tipo = it.tipo.paraDominio(),
                categoria = it.categoria.takeIf(String::isNotBlank),
            )
        },
        ignorados = linhasIgnoradasList.map { LinhaIgnorada(it.posicao, it.motivo) },
    )
} catch (e: IllegalArgumentException) {
    // Resposta que fere o contrato (data ou enum fora do esperado): o importador está com defeito.
    throw ImportadorIndisponivel(e)
}

private fun FormatoDoContrato.paraTexto(): String = when (this) {
    FormatoDoContrato.FORMATO_OFX -> "OFX"
    FormatoDoContrato.FORMATO_CSV -> "CSV"
    FormatoDoContrato.FORMATO_PDF -> "PDF"
    else -> throw IllegalArgumentException("formato fora do contrato: $this")
}

private fun TipoDoContrato.paraDominio(): TipoDoDominio = when (this) {
    TipoDoContrato.TIPO_RECEITA -> TipoDoDominio.RECEITA
    TipoDoContrato.TIPO_DESPESA -> TipoDoDominio.DESPESA
    else -> throw IllegalArgumentException("tipo fora do contrato: $this")
}
