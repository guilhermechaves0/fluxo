package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.aplicacao.FiltroDeContas
import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.aplicacao.RepositorioDeContas
import br.ufrn.fluxo.dominio.Conta
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID

/**
 * A porta [RepositorioDeContas] sobre o PostgreSQL, com o Exposed.
 *
 * Toda operação roda em `suspendTransaction`, para a corrotina não prender a thread do servidor
 * enquanto espera o banco.
 */
class ContasPostgres(private val banco: Database) : RepositorioDeContas {
    override suspend fun listar(filtro: FiltroDeContas, pedido: PedidoDePagina): Pagina<Conta> =
        suspendTransaction(banco) {
            val onde = condicao(filtro)
            val total = Contas.selectAll().where(onde).count()
            // A fatia sai do SQL, com ORDER BY, LIMIT e OFFSET. O índice único em lower(nome) garante
            // que dois nomes não empatam, então a mesma conta não aparece em duas páginas.
            val itens =
                Contas
                    .selectAll()
                    .where(onde)
                    .orderBy(Contas.nome.lowerCase() to SortOrder.ASC)
                    .limit(pedido.tamanho)
                    .offset(pedido.deslocamento)
                    .map { it.paraConta() }
            Pagina(itens, pedido, total)
        }

    override suspend fun buscar(id: String): Conta? = suspendTransaction(banco) {
        Contas
            .selectAll()
            .where { Contas.id eq id }
            .singleOrNull()
            ?.paraConta()
    }

    override suspend fun criar(nome: String): Conta = traduzindoConflito(nomeRepetido(nome)) {
        val nova = Conta(id = UUID.randomUUID().toString(), nome = nome)
        suspendTransaction(banco) {
            Contas.insert {
                it[Contas.id] = nova.id
                it[Contas.nome] = nova.nome
            }
        }
        nova
    }

    override suspend fun renomear(id: String, nome: String): Conta? = traduzindoConflito(nomeRepetido(nome)) {
        suspendTransaction(banco) {
            val alteradas = Contas.update({ Contas.id eq id }) { it[Contas.nome] = nome }
            if (alteradas == 0) null else Conta(id = id, nome = nome)
        }
    }

    override suspend fun remover(id: String): Boolean = suspendTransaction(banco) {
        Contas.deleteWhere { Contas.id eq id } > 0
    }

    private fun condicao(filtro: FiltroDeContas): Op<Boolean> {
        val trecho = filtro.nome ?: return Op.TRUE
        return Contas.nome.lowerCase() like "%${escaparLike(trecho.lowercase())}%"
    }

    private fun nomeRepetido(nome: String) = "já existe uma conta com o nome \"$nome\""

    private fun ResultRow.paraConta() = Conta(id = this[Contas.id], nome = this[Contas.nome])
}
