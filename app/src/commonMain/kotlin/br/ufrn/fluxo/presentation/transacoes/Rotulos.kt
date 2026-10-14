package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarReais

/** Nome do tipo como aparece na tela. */
val Tipo.rotulo: String
    get() =
        when (this) {
            Tipo.RECEITA -> "Receita"
            Tipo.DESPESA -> "Despesa"
        }

/**
 * O valor como o leitor de tela diz: "entrada de R$ 4.500,00" ou "gasto de R$ 45,90". Na tela, a cor e
 * o "+" separam os dois, e o leitor não transmite nenhum dos dois.
 */
val Transacao.leituraDoValor: String
    get() = (if (tipo == Tipo.RECEITA) "entrada de " else "gasto de ") + formatarReais(valorCentavos)
