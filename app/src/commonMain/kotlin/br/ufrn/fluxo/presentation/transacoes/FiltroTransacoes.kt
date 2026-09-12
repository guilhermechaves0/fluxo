package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.dominio.Tipo

/** Filtros da tela de transações. Com [tipo] nulo, a tela mostra todas. */
enum class FiltroTransacoes(val rotulo: String, val tipo: Tipo?) {
    TODAS("Todas", null),
    RECEITAS("Receitas", Tipo.RECEITA),
    DESPESAS("Despesas", Tipo.DESPESA),
}
