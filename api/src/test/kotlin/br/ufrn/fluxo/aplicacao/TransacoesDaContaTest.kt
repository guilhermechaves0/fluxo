package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.ContasEmMemoria
import br.ufrn.fluxo.TransacoesEmMemoria
import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Conta
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Tipo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Os casos de uso das transações com as portas em memória: as regras testadas sem servidor e sem banco. */
class TransacoesDaContaTest {
    private val transacoes =
        TransacoesDaConta(ContasEmMemoria(Conta("c-1", "Nubank"), Conta("c-2", "Carteira")), TransacoesEmMemoria())

    private fun despesa(descricao: String, centavos: Long, dia: String, categoria: Categoria? = null) =
        DadosDaTransacao(descricao, centavos, LocalDate.parse(dia), Tipo.DESPESA, categoria)

    @Test
    fun criarGuardaADescricaoSemOsEspacosDasPontas() = runTest {
        val criada = transacoes.criar("c-1", despesa("  Padaria  ", 4_590, "2026-09-10"))
        assertEquals("Padaria", criada.descricao)
        assertEquals(criada, transacoes.buscar("c-1", criada.id))
    }

    @Test
    fun dadosInvalidosSaoRecusadosComTodasAsViolacoes() = runTest {
        val erro = assertFailsWith<EntradaInvalida> { transacoes.criar("c-1", despesa("   ", 0, "2026-09-10")) }
        assertEquals(listOf("a descrição é obrigatória", "o valor deve ser maior que zero"), erro.violacoes)
    }

    @Test
    fun contaInexistenteEhNaoEncontradoDaContaENaoDaTransacao() = runTest {
        val erro = assertFailsWith<NaoEncontrado> { transacoes.criar("c-99", despesa("Padaria", 4_590, "2026-09-10")) }
        assertEquals("conta c-99 não existe", erro.message)
        assertFailsWith<NaoEncontrado> { transacoes.listar("c-99", FiltroDeTransacoes(), PedidoDePagina()) }
    }

    @Test
    fun transacaoSoEhAchadaPelaContaDela() = runTest {
        val criada = transacoes.criar("c-1", despesa("Padaria", 4_590, "2026-09-10"))
        val erro = assertFailsWith<NaoEncontrado> { transacoes.buscar("c-2", criada.id) }
        assertEquals("transação ${criada.id} não existe", erro.message)
        assertFailsWith<NaoEncontrado> { transacoes.remover("c-2", criada.id) }
        assertFailsWith<NaoEncontrado> { transacoes.atualizar("c-2", criada.id, despesa("Outra", 100, "2026-09-10")) }
    }

    @Test
    fun listagemVemDaDataMaisRecenteParaAMaisAntiga() = runTest {
        transacoes.criar("c-1", despesa("Aluguel", 150_000, "2026-09-05"))
        transacoes.criar("c-1", despesa("Padaria", 4_590, "2026-09-10"))
        transacoes.criar("c-1", despesa("Farmácia", 8_000, "2026-09-10"))
        val pagina = transacoes.listar("c-1", FiltroDeTransacoes(), PedidoDePagina())
        assertEquals(listOf("Farmácia", "Padaria", "Aluguel"), pagina.itens.map { it.descricao })
        assertEquals(3, pagina.total)
    }

    @Test
    fun filtrosSeCombinam() = runTest {
        val mercado = Categoria("mercado", "Mercado")
        transacoes.criar("c-1", despesa("Supermercado Bom Preço", 31_245, "2026-09-06", mercado))
        transacoes.criar("c-1", despesa("Supermercado de agosto", 20_000, "2026-08-20", mercado))
        transacoes.criar("c-1", despesa("Padaria", 4_590, "2026-09-10"))
        transacoes.criar("c-1", DadosDaTransacao("Salário", 450_000, LocalDate.parse("2026-09-05"), Tipo.RECEITA))

        suspend fun descricoes(filtro: FiltroDeTransacoes) =
            transacoes.listar("c-1", filtro, PedidoDePagina()).itens.map { it.descricao }

        assertEquals(listOf("Salário"), descricoes(FiltroDeTransacoes(tipo = Tipo.RECEITA)))
        assertEquals(listOf("Supermercado de agosto"), descricoes(FiltroDeTransacoes(mes = Mes(2026, 8))))
        val setembroNoMercado = FiltroDeTransacoes(mes = Mes(2026, 9), categoria = "mercado")
        assertEquals(listOf("Supermercado Bom Preço"), descricoes(setembroNoMercado))
        assertEquals(listOf("Padaria"), descricoes(FiltroDeTransacoes(descricao = "PADA")))
    }

    @Test
    fun atualizarSubstituiOsDadosERemoverTiraDaConta() = runTest {
        val criada = transacoes.criar("c-1", despesa("Padaria", 4_590, "2026-09-10"))
        val atualizada = transacoes.atualizar("c-1", criada.id, despesa("Padaria da esquina", 5_000, "2026-09-11"))
        assertEquals(criada.id, atualizada.id)
        assertEquals("Padaria da esquina", transacoes.buscar("c-1", criada.id).descricao)

        transacoes.remover("c-1", criada.id)
        assertFailsWith<NaoEncontrado> { transacoes.buscar("c-1", criada.id) }
    }
}
