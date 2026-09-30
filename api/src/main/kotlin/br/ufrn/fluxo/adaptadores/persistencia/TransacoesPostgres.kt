package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.aplicacao.DadosDaTransacao
import br.ufrn.fluxo.aplicacao.FiltroDeTransacoes
import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.aplicacao.RepositorioDeTransacoes
import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * A porta [RepositorioDeTransacoes] sobre o PostgreSQL. Toda consulta leva o id da conta: uma
 * transação só aparece pela conta a que pertence.
 */
class TransacoesPostgres(private val banco: Database) : RepositorioDeTransacoes {
    override suspend fun listar(
        contaId: String,
        filtro: FiltroDeTransacoes,
        pedido: PedidoDePagina,
    ): Pagina<Transacao> = suspendTransaction(banco) {
        val onde = condicao(contaId, filtro)
        val total = Transacoes.selectAll().where(onde).count()
        // A fatia sai do SQL. Dentro do mesmo dia vem primeiro a transação gravada por último, e o id
        // desempata, para a mesma transação não aparecer em duas páginas.
        val itens =
            Transacoes
                .selectAll()
                .where(onde)
                .orderBy(
                    Transacoes.data to SortOrder.DESC,
                    Transacoes.criadoEm to SortOrder.DESC,
                    Transacoes.id to SortOrder.ASC,
                ).limit(pedido.tamanho)
                .offset(pedido.deslocamento)
                .map { it.paraTransacao() }
        Pagina(itens, pedido, total)
    }

    override suspend fun buscar(contaId: String, id: String): Transacao? = suspendTransaction(banco) {
        Transacoes
            .selectAll()
            .where(daConta(contaId, id))
            .singleOrNull()
            ?.paraTransacao()
    }

    override suspend fun criar(contaId: String, dados: DadosDaTransacao): Transacao = exigindoConta(contaId) {
        val id = UUID.randomUUID().toString()
        suspendTransaction(banco) {
            Transacoes.insert {
                it[Transacoes.id] = id
                it[Transacoes.contaId] = contaId
                gravar(it, dados)
            }
        }
        dados.paraTransacao(id)
    }

    override suspend fun atualizar(contaId: String, id: String, dados: DadosDaTransacao): Transacao? =
        suspendTransaction(banco) {
            val alteradas = Transacoes.update({ daConta(contaId, id) }) { gravar(it, dados) }
            if (alteradas == 0) null else dados.paraTransacao(id)
        }

    override suspend fun remover(contaId: String, id: String): Boolean = suspendTransaction(banco) {
        Transacoes.deleteWhere { daConta(contaId, id) } > 0
    }

    private fun daConta(contaId: String, id: String): Op<Boolean> =
        (Transacoes.id eq id) and (Transacoes.contaId eq contaId)

    /** Os filtros viram WHERE, combinados com E. */
    private fun condicao(contaId: String, filtro: FiltroDeTransacoes): Op<Boolean> = listOfNotNull(
        Transacoes.contaId eq contaId,
        filtro.tipo?.let { Transacoes.tipo eq it },
        filtro.mes?.let { noMes(it) },
        filtro.categoria?.let { Transacoes.categoriaId eq it },
        filtro.descricao?.let { Transacoes.descricao.lowerCase() like "%${escaparLike(it.lowercase())}%" },
    ).reduce { a, b -> a and b }

    /** Do primeiro dia do mês, inclusive, até o primeiro dia do mês seguinte, exclusive. */
    private fun noMes(mes: Mes): Op<Boolean> {
        val seguinte = mes.seguinte()
        val inicio = LocalDate(mes.ano, mes.numero, 1)
        val fim = LocalDate(seguinte.ano, seguinte.numero, 1)
        return (Transacoes.data greaterEq inicio) and (Transacoes.data less fim)
    }
}

// Conversões entre a linha da tabela e o domínio, num lugar só.

private fun gravar(linha: UpdateBuilder<*>, dados: DadosDaTransacao) {
    linha[Transacoes.descricao] = dados.descricao
    linha[Transacoes.valorCentavos] = dados.valorCentavos
    linha[Transacoes.data] = dados.data
    linha[Transacoes.tipo] = dados.tipo
    linha[Transacoes.categoriaId] = dados.categoria?.id
    linha[Transacoes.categoriaNome] = dados.categoria?.nome
}

private fun DadosDaTransacao.paraTransacao(id: String) = Transacao(id, descricao, valorCentavos, data, tipo, categoria)

private fun ResultRow.paraTransacao() = Transacao(
    id = this[Transacoes.id],
    descricao = this[Transacoes.descricao],
    valorCentavos = this[Transacoes.valorCentavos],
    data = this[Transacoes.data],
    tipo = this[Transacoes.tipo],
    categoria = paraCategoria(),
)

/** As duas colunas da categoria andam juntas: ou as duas têm valor, ou nenhuma tem. */
private fun ResultRow.paraCategoria(): Categoria? {
    val id = this[Transacoes.categoriaId]
    val nome = this[Transacoes.categoriaNome]
    return if (id != null && nome != null) Categoria(id, nome) else null
}
