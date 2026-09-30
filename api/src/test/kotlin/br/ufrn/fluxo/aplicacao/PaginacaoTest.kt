package br.ufrn.fluxo.aplicacao

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PaginacaoTest {
    @Test
    fun semParametrosEhAPrimeiraPaginaComOTamanhoPadrao() {
        val pedido = PedidoDePagina()
        assertEquals(0, pedido.pagina)
        assertEquals(PedidoDePagina.TAMANHO_PADRAO, pedido.tamanho)
        assertEquals(0, pedido.deslocamento)
    }

    @Test
    fun deslocamentoPulaAsPaginasAnteriores() {
        assertEquals(40, PedidoDePagina(pagina = 2, tamanho = 20).deslocamento)
    }

    @Test
    fun tamanhoTemTetoEPisoEAPaginaNaoEhNegativa() {
        assertFailsWith<EntradaInvalida> { PedidoDePagina(tamanho = PedidoDePagina.TAMANHO_MAXIMO + 1) }
        assertFailsWith<EntradaInvalida> { PedidoDePagina(tamanho = 0) }
        val erro = assertFailsWith<EntradaInvalida> { PedidoDePagina(pagina = -1, tamanho = 0) }
        assertEquals(2, erro.violacoes.size, erro.violacoes.toString())
    }
}
