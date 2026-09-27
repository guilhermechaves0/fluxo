package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Tipo
import kotlinx.datetime.LocalDate

/** Porta de saída para o serviço que lê extratos OFX e CSV. A implementação fica em adaptadores/importador. */
interface ImportadorDeExtratos {
    suspend fun ler(conteudo: ByteArray): ExtratoLido
}

/** O que o importador entende do arquivo: formato, banco reconhecido, lançamentos e linhas descartadas. */
data class ExtratoLido(
    val formato: String,
    val origem: String,
    val lancamentos: List<LancamentoLido>,
    val ignorados: List<LinhaIgnorada>,
)

data class LancamentoLido(
    val idExterno: String,
    val data: LocalDate,
    val valorCentavos: Long,
    val descricao: String,
    val tipo: Tipo,
    /** Como o banco classifica o lançamento, quando o extrato informa. */
    val categoria: String? = null,
)

/** Bloco OFX ou linha CSV que não virou lançamento. [posicao] conta a partir de 1. */
data class LinhaIgnorada(val posicao: Int, val motivo: String)

/** O arquivo chegou, mas o importador não reconheceu o formato ou o conteúdo. */
class ExtratoInvalido(motivo: String) : RuntimeException(motivo)

/** O importador não respondeu ou respondeu com erro dele. */
class ImportadorIndisponivel(causa: Throwable? = null) :
    RuntimeException("O importador de extratos não respondeu.", causa)
