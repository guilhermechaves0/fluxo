package br.ufrn.fluxo.dominio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ValidacaoTest {
    @Test
    fun nomeDeContaComTextoEhValido() {
        assertTrue(violacoesDaConta("Nubank").isEmpty())
        assertTrue(violacoesDaConta("  Carteira  ").isEmpty())
    }

    @Test
    fun nomeDeContaVazioOuSoComEspacosEhRecusado() {
        assertEquals(listOf("o nome da conta é obrigatório"), violacoesDaConta(""))
        assertEquals(listOf("o nome da conta é obrigatório"), violacoesDaConta("   "))
    }

    @Test
    fun nomeDeContaTemTamanhoMaximoSemContarOsEspacosDasPontas() {
        val noLimite = "a".repeat(TAMANHO_MAXIMO_DO_NOME_DA_CONTA)
        assertTrue(violacoesDaConta(" $noLimite ").isEmpty())
        assertEquals(listOf("o nome da conta passa de 60 caracteres"), violacoesDaConta(noLimite + "a"))
    }

    @Test
    fun transacaoComDescricaoEValorPositivoEhValida() {
        assertTrue(violacoesDaTransacao("Padaria", 4_590).isEmpty())
        assertTrue(violacoesDaTransacao("Mercado", 1, Categoria("mercado", "Mercado")).isEmpty())
    }

    @Test
    fun transacaoDevolveTodasAsViolacoesDeUmaVez() {
        val categoriaLonga = Categoria("x", "c".repeat(TAMANHO_MAXIMO_DA_CATEGORIA + 1))
        val violacoes = violacoesDaTransacao("  ", 0, categoriaLonga)
        assertEquals(
            listOf(
                "a descrição é obrigatória",
                "o valor deve ser maior que zero",
                "a categoria passa de 60 caracteres",
            ),
            violacoes,
        )
    }

    @Test
    fun descricaoDaTransacaoTemTamanhoMaximo() {
        val noLimite = "d".repeat(TAMANHO_MAXIMO_DA_DESCRICAO)
        assertTrue(violacoesDaTransacao(noLimite, 100).isEmpty())
        assertEquals(listOf("a descrição passa de 200 caracteres"), violacoesDaTransacao(noLimite + "d", 100))
    }
}
