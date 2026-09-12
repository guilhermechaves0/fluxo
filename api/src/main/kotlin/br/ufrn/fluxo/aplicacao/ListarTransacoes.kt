package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Transacao

/** Caso de uso de listagem, sem dependência de framework. */
class ListarTransacoes(private val fonte: FonteDeTransacoes) {
    /** Devolve as transações da mais recente para a mais antiga. */
    suspend operator fun invoke(): List<Transacao> = fonte.listar().sortedByDescending { it.data }
}
