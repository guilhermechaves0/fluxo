package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.data.novaTransacaoDeExemplo
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.porTipo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FiltroTransacoesTest {
    @Test
    fun todasNaoFiltraNada() {
        val todas = transacoesDeExemplo.porTipo(FiltroTransacoes.TODAS.tipo)
        assertEquals(transacoesDeExemplo.size, todas.size)
    }

    @Test
    fun despesasSoDevolveDespesas() {
        val despesas = transacoesDeExemplo.porTipo(FiltroTransacoes.DESPESAS.tipo)
        assertTrue(despesas.isNotEmpty())
        assertTrue(despesas.all { it.tipo == Tipo.DESPESA })
    }

    @Test
    fun cadaNovaTransacaoDeExemploTemIdProprio() {
        assertNotEquals(novaTransacaoDeExemplo(1).id, novaTransacaoDeExemplo(2).id)
    }
}
