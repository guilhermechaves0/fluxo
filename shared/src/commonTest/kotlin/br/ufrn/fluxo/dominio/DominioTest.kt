package br.ufrn.fluxo.dominio

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals("1.234,56", formatarValor(-123_456))
    }

    @Test
    fun valorNaoPositivoERecusado() {
        assertFailsWith<IllegalArgumentException> {
            Transacao("t4", "Estorno", 0, LocalDate.parse("2026-09-10"), Tipo.DESPESA)
        }
    }

    @Test
    fun leReaisNosFormatosQueOUsuarioDigita() {
        assertEquals(1_200L, lerReais("12"))
        assertEquals(1_250L, lerReais("12,5"))
        assertEquals(123_456L, lerReais("1.234,56"))
        assertEquals(4_590L, lerReais("R$ 45,90"))
        assertEquals(4_590L, lerReais("45.90"))
        assertEquals(123_400L, lerReais("1.234"))
    }

    @Test
    fun recusaTextoQueNaoEValor() {
        assertNull(lerReais(""))
        assertNull(lerReais("abc"))
        assertNull(lerReais("-45,90"))
        assertNull(lerReais("12,345"))
        assertNull(lerReais(","))
    }

    @Test
    fun formataELeDatasNoPadraoBrasileiro() {
        assertEquals("05/09/2026", formatarData(LocalDate.parse("2026-09-05")))
        assertEquals(LocalDate.parse("2026-09-27"), lerData("27/09/2026"))
        assertNull(lerData("31/02/2026"))
        assertNull(lerData("2026-09-27"))
        assertNull(lerData("27/09/26"))
    }

    @Test
    fun mesAnteriorESeguinteAtravessamOAno() {
        assertEquals(Mes(2025, 12), Mes(2026, 1).anterior())
        assertEquals(Mes(2027, 1), Mes(2026, 12).seguinte())
        assertTrue(Mes(2026, 8) < Mes(2026, 9))
        assertFailsWith<IllegalArgumentException> { Mes(2026, 13) }
    }

    @Test
    fun doMesSoDevolveAsTransacoesDaquelesMesEFormataONome() {
        val agosto = Transacao("t4", "Mercado de agosto", 5_000, LocalDate.parse("2026-08-30"), Tipo.DESPESA)
        val todas = transacoes + agosto
        assertEquals(listOf("t4"), todas.doMes(Mes(2026, 8)).map { it.id })
        assertEquals(3, todas.doMes(LocalDate.parse("2026-09-27").mesDoAno).size)
        assertEquals("março de 2026", formatarMes(Mes(2026, 3)))
    }

    @Test
    fun categoriaPeloNomeGeraOMesmoIdSemAcento() {
        assertEquals(
            Categoria("pagamentos-creditos", "Pagamentos/Créditos"),
            categoriaPeloNome(" Pagamentos/Créditos "),
        )
        assertEquals("saude", categoriaPeloNome("Saúde").id)
        assertEquals("outros", categoriaPeloNome("***").id)
    }
}
