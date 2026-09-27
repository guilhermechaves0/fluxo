package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.dominio.Tipo

/** Nome do tipo como aparece na tela. */
val Tipo.rotulo: String
    get() =
        when (this) {
            Tipo.RECEITA -> "Receita"
            Tipo.DESPESA -> "Despesa"
        }
