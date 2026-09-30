package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate

/**
 * Porta de saída das transações. Toda operação é dentro de uma conta: uma transação só é achada,
 * alterada ou removida pela conta a que pertence.
 */
interface RepositorioDeTransacoes {
    /** Da data mais recente para a mais antiga. */
    suspend fun listar(contaId: String, filtro: FiltroDeTransacoes, pedido: PedidoDePagina): Pagina<Transacao>

    suspend fun buscar(contaId: String, id: String): Transacao?

    /** Lança [NaoEncontrado] se a conta não existe. */
    suspend fun criar(contaId: String, dados: DadosDaTransacao): Transacao

    /** Devolve `null` se a transação não existe nessa conta. */
    suspend fun atualizar(contaId: String, id: String, dados: DadosDaTransacao): Transacao?

    /** Devolve `false` se a transação não existe nessa conta. */
    suspend fun remover(contaId: String, id: String): Boolean
}

/** O que se informa para criar ou substituir uma transação. O id é o servidor quem atribui. */
data class DadosDaTransacao(
    val descricao: String,
    val valorCentavos: Long,
    val data: LocalDate,
    val tipo: Tipo,
    val categoria: Categoria? = null,
)

/** Filtros da listagem, todos opcionais e combinados com E. */
data class FiltroDeTransacoes(
    val tipo: Tipo? = null,
    /** Só as transações com data neste mês. */
    val mes: Mes? = null,
    /** Id da categoria, como `mercado`. */
    val categoria: String? = null,
    /** A descrição contém este texto, sem diferenciar maiúsculas de minúsculas. */
    val descricao: String? = null,
)
