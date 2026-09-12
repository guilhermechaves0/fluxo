package br.ufrn.fluxo.dominio

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DominioTest {
    private val mercado = Categoria(id = "mercado", nome = "Mercado")

    private val transacoes =
        listOf(
            Transacao("t1", "Salário", 450_000, LocalDate.parse("2026-09-05"), Tipo.RECEITA),
            Transacao("t2", "Supermercado", 31_245, LocalDate.parse("2026-09-06"), Tipo.DESPESA, mercado),
            Transacao("t3", "Padaria", 4_590, LocalDate.parse("2026-09-10"), Tipo.DESPESA),
        )

    @Test
    fun saldoSomaReceitasESubtraiDespesas() {
        assertEquals(450_000L - 31_245L - 4_590L, transacoes.saldo())
    }

    @Test
    fun porTipoDevolveSoOTipoPedido() {
        assertEquals(3, transacoes.porTipo(null).size)
        assertEquals(listOf("t2", "t3"), transacoes.porTipo(Tipo.DESPESA).map { it.id })
    }

    @Test
    fun despesasPorCategoriaGuardaAsSemCategoriaEmNull() {
        assertEquals(mapOf(mercado to 31_245L, null to 4_590L), transacoes.despesasPorCategoria())
    }

    @Test
    fun formataReaisComMilharECentavos() {
        assertEquals("R\$ 1.234,56", formatarReais(123_456))
        assertEquals("-R\$ 45,90", formatarReais(-4_590))
        assertEquals("R\$ 0,05", formatarReais(5))
    }

    @Test
    fun valorNaoPositivoERecusado() {
        assertFailsWith<IllegalArgumentException> {
            Transacao("t4", "Estorno", 0, LocalDate.parse("2026-09-10"), Tipo.DESPESA)
        }
    }
}
