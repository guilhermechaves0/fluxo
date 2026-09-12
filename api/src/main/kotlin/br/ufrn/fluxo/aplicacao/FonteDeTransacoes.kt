package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Transacao

/** Porta de saída para buscar transações. As implementações ficam em adaptadores/. */
interface FonteDeTransacoes {
    suspend fun listar(): List<Transacao>
}
