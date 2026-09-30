package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Conta

/**
 * Porta de saída das contas. As operações têm o nome do que a aplicação precisa e não falam de SQL.
 * São `suspend` porque a implementação com banco faz entrada e saída e não pode prender a thread.
 */
interface RepositorioDeContas {
    suspend fun listar(filtro: FiltroDeContas, pedido: PedidoDePagina): Pagina<Conta>

    suspend fun buscar(id: String): Conta?

    /** Lança [Conflito] se já existe uma conta com o mesmo nome. */
    suspend fun criar(nome: String): Conta

    /** Devolve `null` se a conta não existe. Lança [Conflito] se o nome já é de outra conta. */
    suspend fun renomear(id: String, nome: String): Conta?

    /** Devolve `false` se a conta não existe. */
    suspend fun remover(id: String): Boolean
}

/** Filtro da listagem: o nome contém [nome], sem diferenciar maiúsculas de minúsculas. */
data class FiltroDeContas(val nome: String? = null)
