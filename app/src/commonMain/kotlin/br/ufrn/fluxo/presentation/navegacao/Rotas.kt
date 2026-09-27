package br.ufrn.fluxo.presentation.navegacao

import kotlinx.serialization.Serializable

// Destinos do app. Cada destino é um tipo, e os argumentos são propriedades: a navegação passa
// só o id da transação, e a tela busca os dados no estado.

@Serializable
data object ListaDeTransacoes

@Serializable
data class DetalheDaTransacao(val id: String)

@Serializable
data object NovaTransacao

/** Base do deep link do detalhe: fluxo://transacao/t-03 abre a transação t-03. */
const val ENDERECO_DO_DETALHE = "fluxo://transacao"
