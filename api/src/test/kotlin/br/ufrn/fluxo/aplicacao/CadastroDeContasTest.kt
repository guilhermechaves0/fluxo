package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.ContasEmMemoria
import br.ufrn.fluxo.dominio.Conta
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Os casos de uso com a porta em memória: as regras testadas sem servidor e sem banco. */
class CadastroDeContasTest {
    private val cadastro = CadastroDeContas(ContasEmMemoria(Conta("c-1", "Nubank"), Conta("c-2", "Carteira")))

    @Test
    fun criarGuardaONomeSemOsEspacosDasPontas() = runTest {
        val criada = cadastro.criar("  Banco do Brasil  ")
        assertEquals("Banco do Brasil", criada.nome)
        assertEquals(criada, cadastro.buscar(criada.id))
    }

    @Test
    fun nomeVazioEhRecusadoAntesDeChegarAoRepositorio() = runTest {
        val erro = assertFailsWith<EntradaInvalida> { cadastro.criar("   ") }
        assertEquals(listOf("o nome da conta é obrigatório"), erro.violacoes)
        assertFailsWith<EntradaInvalida> { cadastro.renomear("c-1", "") }
    }

    @Test
    fun contaInexistenteEhNaoEncontrado() = runTest {
        val erro = assertFailsWith<NaoEncontrado> { cadastro.buscar("c-99") }
        assertEquals("conta c-99 não existe", erro.message)
        assertFailsWith<NaoEncontrado> { cadastro.renomear("c-99", "Outra") }
        assertFailsWith<NaoEncontrado> { cadastro.remover("c-99") }
    }

    @Test
    fun nomeRepetidoEhConflitoMesmoComOutraCaixa() = runTest {
        assertFailsWith<Conflito> { cadastro.criar("NUBANK") }
        assertFailsWith<Conflito> { cadastro.renomear("c-2", "nubank") }
    }

    @Test
    fun renomearParaOProprioNomeNaoEhConflito() = runTest {
        assertEquals(Conta("c-1", "NuBank"), cadastro.renomear("c-1", "NuBank"))
    }

    @Test
    fun removerTiraAContaDaListagem() = runTest {
        cadastro.remover("c-1")
        val pagina = cadastro.listar(FiltroDeContas(), PedidoDePagina())
        assertEquals(listOf("Carteira"), pagina.itens.map { it.nome })
        assertEquals(1, pagina.total)
    }
}
