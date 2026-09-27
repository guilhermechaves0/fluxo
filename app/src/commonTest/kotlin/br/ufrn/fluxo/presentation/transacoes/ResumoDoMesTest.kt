package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ResumoDoMesTest {
    private val hoje = LocalDate(2026, 9, 27)

    @Test
    fun fatiasSomamSoOsGastosDoMaiorParaOMenor() {
        val fatias = transacoesDeExemplo.fatiasDeGasto()
        assertEquals(listOf("moradia", "mercado", "alimentacao", "transporte"), fatias.map { it.id })
        assertEquals(listOf("80%", "17%", "2%", "1%"), fatias.map { it.percentual })
        assertEquals(1f, fatias.sumOf { it.fracao.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun gastoSemCategoriaViraUmaFatiaPropria() {
        val semCategoria = Transacao("x", "Tarifa", 1_000, hoje, Tipo.DESPESA)
        val fatias = (transacoesDeExemplo + semCategoria).fatiasDeGasto()
        assertEquals("Sem categoria", fatias.single { it.id == ID_SEM_CATEGORIA }.nome)
        assertEquals(listOf("x"), (transacoesDeExemplo + semCategoria).daCategoria(ID_SEM_CATEGORIA).map { it.id })
    }

    @Test
    fun filtroPorCategoriaENuloMostraTudo() {
        assertEquals(listOf("t-02"), transacoesDeExemplo.daCategoria("moradia").map { it.id })
        assertEquals(transacoesDeExemplo, transacoesDeExemplo.daCategoria(null))
    }

    @Test
    fun rotuloDoDiaUsaHojeOntemEOAnoSoQuandoMuda() {
        assertEquals("Hoje", rotuloDoDia(hoje, hoje))
        assertEquals("Ontem", rotuloDoDia(LocalDate(2026, 9, 26), hoje))
        assertEquals("10 de setembro", rotuloDoDia(LocalDate(2026, 9, 10), hoje))
        assertEquals("30 de dezembro de 2025", rotuloDoDia(LocalDate(2025, 12, 30), hoje))
    }

    @Test
    fun diasVemDoMaisRecenteParaOMaisAntigo() {
        val dias = transacoesDeExemplo.porDia().map { it.first.day }
        assertEquals(listOf(10, 9, 8, 6, 5), dias)
    }
}
