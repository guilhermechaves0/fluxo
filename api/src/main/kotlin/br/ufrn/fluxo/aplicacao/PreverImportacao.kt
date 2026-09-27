package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Transacao

private const val TAMANHO_MAXIMO = 5 * 1024 * 1024

/**
 * Caso de uso da prévia de importação: lê o extrato e devolve as transações que ele geraria, sem
 * gravar nada. O app mostra a prévia e decide o que importar.
 */
class PreverImportacao(private val importador: ImportadorDeExtratos) {
    suspend operator fun invoke(conteudo: ByteArray): PreviaDeImportacao {
        require(conteudo.isNotEmpty()) { "O arquivo está vazio." }
        if (conteudo.size > TAMANHO_MAXIMO) throw ArquivoGrandeDemais()
        val extrato = importador.ler(conteudo)
        val prefixo = "imp-" + extrato.origem.substringBefore(" (").lowercase().filter { it.isLetterOrDigit() }
        val (validos, semValor) = extrato.lancamentos.partition { it.valorCentavos > 0 }
        return PreviaDeImportacao(
            formato = extrato.formato,
            origem = extrato.origem,
            transacoes =
            validos.map {
                Transacao(
                    id = "$prefixo-${it.idExterno}",
                    descricao = it.descricao.ifBlank { "Sem descrição" },
                    valorCentavos = it.valorCentavos,
                    data = it.data,
                    tipo = it.tipo,
                )
            },
            ignorados = extrato.ignorados + semValor.map { LinhaIgnorada(posicao = 0, motivo = "valor zero") },
        )
    }
}

data class PreviaDeImportacao(
    val formato: String,
    val origem: String,
    val transacoes: List<Transacao>,
    val ignorados: List<LinhaIgnorada>,
)

/** O extrato passa de 5 MB. */
class ArquivoGrandeDemais : RuntimeException("O extrato passa de 5 MB.")
