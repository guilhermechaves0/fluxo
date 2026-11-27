package br.ufrn.fluxo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ComandosTest {
    @Test
    fun semArgumentoServe() {
        assertEquals(Comando.SERVIR, escolherComando(emptyArray()))
    }

    @Test
    fun servirExplicitoServe() {
        assertEquals(Comando.SERVIR, escolherComando(arrayOf("servir")))
    }

    @Test
    fun migrarSoMigra() {
        assertEquals(Comando.MIGRAR, escolherComando(arrayOf("migrar")))
    }

    @Test
    fun comandoDesconhecidoEErroDeUso() {
        assertFailsWith<IllegalArgumentException> { escolherComando(arrayOf("apagar")) }
    }
}
