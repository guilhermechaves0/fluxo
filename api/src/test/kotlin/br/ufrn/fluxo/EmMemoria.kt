package br.ufrn.fluxo

import br.ufrn.fluxo.aplicacao.Conflito
import br.ufrn.fluxo.aplicacao.DadosDaTransacao
import br.ufrn.fluxo.aplicacao.ExtratoLido
import br.ufrn.fluxo.aplicacao.FiltroDeContas
import br.ufrn.fluxo.aplicacao.FiltroDeTransacoes
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.aplicacao.RepositorioDeContas
import br.ufrn.fluxo.aplicacao.RepositorioDeTransacoes
import br.ufrn.fluxo.dominio.Conta
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.mesDoAno
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

/** Transações em memória, cada uma com a conta dela e a ordem em que chegou. */
class TransacoesEmMemoria : RepositorioDeTransacoes {
    private data class Guardada(val contaId: String, val ordem: Int, val transacao: Transacao)

    private val guardadas = mutableListOf<Guardada>()
    private var proxima = 1

    override suspend fun listar(
        contaId: String,
        filtro: FiltroDeTransacoes,
        pedido: PedidoDePagina,
    ): Pagina<Transacao> {
        val tipo = filtro.tipo
        val mes = filtro.mes
        val categoria = filtro.categoria
        val trecho = filtro.descricao
        // Como no banco: da data mais recente para a mais antiga e, no mesmo dia, a que chegou por último.
        val filtradas =
            guardadas
                .filter { it.contaId == contaId }
                .sortedWith(compareByDescending<Guardada> { it.transacao.data }.thenByDescending { it.ordem })
                .map { it.transacao }
                .filter { tipo == null || it.tipo == tipo }
                .filter { mes == null || it.data.mesDoAno == mes }
                .filter { categoria == null || it.categoria?.id == categoria }
                .filter { trecho == null || it.descricao.contains(trecho, ignoreCase = true) }
        val itens = filtradas.drop(pedido.deslocamento.toInt()).take(pedido.tamanho)
        return Pagina(itens, pedido, filtradas.size.toLong())
    }

    override suspend fun buscar(contaId: String, id: String): Transacao? =
        guardadas.find { it.contaId == contaId && it.transacao.id == id }?.transacao

    override suspend fun criar(contaId: String, dados: DadosDaTransacao): Transacao {
        val ordem = proxima++
        val nova = dados.paraTransacao("transacao-$ordem")
        guardadas += Guardada(contaId, ordem, nova)
        return nova
    }

    override suspend fun atualizar(contaId: String, id: String, dados: DadosDaTransacao): Transacao? {
        val posicao = guardadas.indexOfFirst { it.contaId == contaId && it.transacao.id == id }
        if (posicao < 0) return null
        val atualizada = dados.paraTransacao(id)
        guardadas[posicao] = guardadas[posicao].copy(transacao = atualizada)
        return atualizada
    }

    override suspend fun remover(contaId: String, id: String): Boolean =
        guardadas.removeIf { it.contaId == contaId && it.transacao.id == id }

    private fun DadosDaTransacao.paraTransacao(id: String) =
        Transacao(id, descricao, valorCentavos, data, tipo, categoria)
}

/** Importador que não lê nada, para os testes que não passam pela importação. */
object ImportadorVazio : ImportadorDeExtratos {
    override suspend fun ler(conteudo: ByteArray) = ExtratoLido("OFX", "Banco de teste", emptyList(), emptyList())
}

/** As portas de persistência em memória, no lugar do PostgreSQL. */
fun persistenciaEmMemoria(
    contas: RepositorioDeContas = ContasEmMemoria(),
    transacoes: RepositorioDeTransacoes = TransacoesEmMemoria(),
) = module {
    single<RepositorioDeContas> { contas }
    single<RepositorioDeTransacoes> { transacoes }
}

/** As dependências da api com dublês no lugar do importador em Go e do banco. */
fun modulosEmMemoria(
    importador: ImportadorDeExtratos = ImportadorVazio,
    contas: RepositorioDeContas = ContasEmMemoria(),
): List<Module> {
    val importadorFalso = module { single<ImportadorDeExtratos> { importador } }
    return listOf(importadorFalso, persistenciaEmMemoria(contas), casosDeUso)
}
