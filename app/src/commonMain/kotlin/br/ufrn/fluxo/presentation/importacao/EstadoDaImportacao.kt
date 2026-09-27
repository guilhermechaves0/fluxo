package br.ufrn.fluxo.presentation.importacao

import br.ufrn.fluxo.data.ExtratoNaApi
import br.ufrn.fluxo.dominio.Transacao

/** Em que passo a importação está. A tela desenha cada passo e não guarda estado. */
sealed interface EstadoDaImportacao {
    data object Inicial : EstadoDaImportacao

    data class Lendo(val nomeDoArquivo: String) : EstadoDaImportacao

    /**
     * [novas] entram na lista ao confirmar; [repetidas] já estavam nela, de uma importação anterior;
     * [ignoradas] traz o motivo de cada linha do arquivo que não virou lançamento.
     */
    data class Previa(
        val nomeDoArquivo: String,
        val origem: String,
        val novas: List<Transacao>,
        val repetidas: Int,
        val ignoradas: List<String>,
    ) : EstadoDaImportacao

    data class Falha(val mensagem: String) : EstadoDaImportacao
}

/** Separa o que é novo do que já está na lista, comparando pelo id que a api monta do extrato. */
fun montarPrevia(nomeDoArquivo: String, extrato: ExtratoNaApi, existentes: List<Transacao>): EstadoDaImportacao.Previa {
    val ids = existentes.map { it.id }.toSet()
    val (repetidas, novas) = extrato.transacoes.partition { it.id in ids }
    return EstadoDaImportacao.Previa(
        nomeDoArquivo = nomeDoArquivo,
        origem = extrato.origem,
        novas = novas.sortedByDescending { it.data },
        repetidas = repetidas.size,
        ignoradas = extrato.ignorados,
    )
}
