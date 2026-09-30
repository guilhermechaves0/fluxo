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
}
