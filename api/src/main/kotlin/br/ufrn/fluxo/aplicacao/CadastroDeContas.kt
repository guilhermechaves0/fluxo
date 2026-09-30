package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Conta
import br.ufrn.fluxo.dominio.violacoesDaConta

/**
 * Casos de uso das contas.
 *
 * Valida a entrada com as regras do domínio antes de chegar ao banco e dá nome ao que deu errado:
 * [EntradaInvalida], [NaoEncontrado] ou [Conflito]. O banco continua com as restrições dele como
 * última barreira.
 */
class CadastroDeContas(private val contas: RepositorioDeContas) {
    suspend fun listar(filtro: FiltroDeContas, pedido: PedidoDePagina): Pagina<Conta> = contas.listar(filtro, pedido)

    suspend fun buscar(id: String): Conta = contas.buscar(id) ?: throw NaoEncontrado("conta", id)

    suspend fun criar(nome: String): Conta = contas.criar(nomeValido(nome))

    suspend fun renomear(id: String, nome: String): Conta =
        contas.renomear(id, nomeValido(nome)) ?: throw NaoEncontrado("conta", id)

    suspend fun remover(id: String) {
        if (!contas.remover(id)) throw NaoEncontrado("conta", id)
    }

    /** O nome sem os espaços das pontas, ou [EntradaInvalida] com tudo o que está errado nele. */
    private fun nomeValido(nome: String): String {
        val violacoes = violacoesDaConta(nome)
        if (violacoes.isNotEmpty()) throw EntradaInvalida(violacoes)
        return nome.trim()
    }
}
