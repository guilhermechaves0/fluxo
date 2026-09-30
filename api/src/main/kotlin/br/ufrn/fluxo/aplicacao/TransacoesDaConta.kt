package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.violacoesDaTransacao

/**
 * Casos de uso das transações, sempre dentro de uma conta (`/contas/{id}/transacoes`).
 *
 * Duas regras além da forma dos dados: mexer em transação de conta que não existe é [NaoEncontrado]
 * da conta, e não da transação; e os dados são validados com as regras do domínio antes de chegar
 * ao banco, com todas as violações de uma vez.
 */
class TransacoesDaConta(private val contas: RepositorioDeContas, private val transacoes: RepositorioDeTransacoes) {
    suspend fun listar(contaId: String, filtro: FiltroDeTransacoes, pedido: PedidoDePagina): Pagina<Transacao> {
        exigirConta(contaId)
        return transacoes.listar(contaId, filtro, pedido)
    }

    suspend fun buscar(contaId: String, id: String): Transacao {
        exigirConta(contaId)
        return transacoes.buscar(contaId, id) ?: throw NaoEncontrado("transação", id)
    }

    suspend fun criar(contaId: String, dados: DadosDaTransacao): Transacao {
        exigirConta(contaId)
        return transacoes.criar(contaId, validos(dados))
    }

    suspend fun atualizar(contaId: String, id: String, dados: DadosDaTransacao): Transacao {
        exigirConta(contaId)
        return transacoes.atualizar(contaId, id, validos(dados)) ?: throw NaoEncontrado("transação", id)
    }

    suspend fun remover(contaId: String, id: String) {
        exigirConta(contaId)
        if (!transacoes.remover(contaId, id)) throw NaoEncontrado("transação", id)
    }

    private suspend fun exigirConta(contaId: String) {
        contas.buscar(contaId) ?: throw NaoEncontrado("conta", contaId)
    }

    /** Os dados com a descrição sem os espaços das pontas, ou [EntradaInvalida] com tudo o que está errado. */
    private fun validos(dados: DadosDaTransacao): DadosDaTransacao {
        val violacoes = violacoesDaTransacao(dados.descricao, dados.valorCentavos, dados.categoria)
        if (violacoes.isNotEmpty()) throw EntradaInvalida(violacoes)
        return dados.copy(descricao = dados.descricao.trim())
    }
}
