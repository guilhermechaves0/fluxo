package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.memoria.TransacoesEmMemoria
import br.ufrn.fluxo.aplicacao.Conflito
import br.ufrn.fluxo.aplicacao.ExtratoLido
import br.ufrn.fluxo.aplicacao.FiltroDeContas
import br.ufrn.fluxo.aplicacao.FonteDeTransacoes
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.aplicacao.RepositorioDeContas
import br.ufrn.fluxo.dominio.Conta
import org.koin.core.module.Module
import org.koin.dsl.module

// Dublês das portas, para testar casos de uso e rotas sem banco nem rede: rodam em milissegundos e
// sem Docker. O comportamento com o banco de verdade é conferido no IntegracaoPostgresTest.

/** Contas numa lista em memória, com a mesma regra de nome único que o banco tem no índice. */
class ContasEmMemoria(vararg iniciais: Conta) : RepositorioDeContas {
    private val contas = iniciais.associateBy { it.id }.toMutableMap()
    private var proximo = 1

    override suspend fun listar(filtro: FiltroDeContas, pedido: PedidoDePagina): Pagina<Conta> {
        val trecho = filtro.nome
        val filtradas =
            contas.values
                .filter { trecho == null || it.nome.contains(trecho, ignoreCase = true) }
                .sortedBy { it.nome.lowercase() }
        val itens = filtradas.drop(pedido.deslocamento.toInt()).take(pedido.tamanho)
        return Pagina(itens, pedido, filtradas.size.toLong())
    }

    override suspend fun buscar(id: String): Conta? = contas[id]

    override suspend fun criar(nome: String): Conta {
        exigirNomeLivre(nome, excetoId = null)
        return Conta(id = "conta-${proximo++}", nome = nome).also { contas[it.id] = it }
    }

    override suspend fun renomear(id: String, nome: String): Conta? {
        if (id !in contas) return null
        exigirNomeLivre(nome, excetoId = id)
        return Conta(id = id, nome = nome).also { contas[id] = it }
    }

    override suspend fun remover(id: String): Boolean = contas.remove(id) != null

    private fun exigirNomeLivre(nome: String, excetoId: String?) {
        if (contas.values.any { it.nome.equals(nome, ignoreCase = true) && it.id != excetoId }) {
            throw Conflito("já existe uma conta com o nome \"$nome\"")
        }
    }
}

/** Importador que não lê nada, para os testes que não passam pela importação. */
object ImportadorVazio : ImportadorDeExtratos {
    override suspend fun ler(conteudo: ByteArray) = ExtratoLido("OFX", "Banco de teste", emptyList(), emptyList())
}

/** As portas de persistência em memória, no lugar do PostgreSQL. */
fun persistenciaEmMemoria(contas: RepositorioDeContas = ContasEmMemoria()) = module {
    single<RepositorioDeContas> { contas }
    single<FonteDeTransacoes> { TransacoesEmMemoria() }
}

/** As dependências da api com dublês no lugar do importador em Go e do banco. */
fun modulosEmMemoria(
    importador: ImportadorDeExtratos = ImportadorVazio,
    contas: RepositorioDeContas = ContasEmMemoria(),
): List<Module> {
    val importadorFalso = module { single<ImportadorDeExtratos> { importador } }
    return listOf(importadorFalso, persistenciaEmMemoria(contas), casosDeUso)
}
