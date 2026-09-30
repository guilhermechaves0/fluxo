package br.ufrn.fluxo.aplicacao

/**
 * Qual fatia da listagem foi pedida. [pagina] começa em 0.
 *
 * O tamanho tem teto para que um pedido só não traga a tabela inteira. A fatia vai para o SQL, em
 * LIMIT e OFFSET: nada é paginado em memória.
 */
data class PedidoDePagina(val pagina: Int = 0, val tamanho: Int = TAMANHO_PADRAO) {
    init {
        val violacoes =
            buildList {
                if (pagina < 0) add("pagina deve ser maior ou igual a 0")
                if (tamanho !in 1..TAMANHO_MAXIMO) add("tamanho deve ficar entre 1 e $TAMANHO_MAXIMO")
            }
        if (violacoes.isNotEmpty()) throw EntradaInvalida(violacoes)
    }

    /** Quantos itens pular até o começo da página. */
    val deslocamento: Long
        get() = pagina.toLong() * tamanho

    companion object {
        const val TAMANHO_PADRAO = 20
        const val TAMANHO_MAXIMO = 100
    }
}

/** Uma fatia da listagem e o total de itens, para quem consome saber quantas páginas existem. */
data class Pagina<T>(val itens: List<T>, val pedido: PedidoDePagina, val total: Long)
