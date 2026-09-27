package br.ufrn.fluxo.presentation.lancamento

import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RascunhoTransacaoTest {
    private val saude = Categoria(id = "saude", nome = "Saúde")
    private val completo =
        RascunhoTransacao(descricao = " Farmácia ", valor = "37,90", data = "27/09/2026", categoria = saude)

    @Test
    fun rascunhoVazioNaoPodeSerSalvoENaoMostraErro() {
        val vazio = RascunhoTransacao()
        assertFalse(vazio.podeSalvar)
        assertEquals(ErrosDoRascunho(), vazio.erros())
    }

    @Test
    fun rascunhoCompletoViraTransacaoComCentavosEDescricaoSemEspacos() {
        val transacao = assertNotNull(completo.paraTransacao(id = "m-1"))
        assertEquals("Farmácia", transacao.descricao)
        assertEquals(3_790L, transacao.valorCentavos)
        assertEquals(LocalDate(2026, 9, 27), transacao.data)
        assertEquals(saude, transacao.categoria)
    }

    @Test
    fun valorEDataInvalidosGeramMensagem() {
        val erros = completo.copy(valor = "abc", data = "31/02/2026").erros()
        assertEquals("Digite um valor como 45,90.", erros.valor)
        assertEquals("Use uma data válida no formato dd/mm/aaaa.", erros.data)
        assertEquals("O valor precisa ser maior que zero.", completo.copy(valor = "0").erros().valor)
    }

    @Test
    fun despesaSemCategoriaNaoPodeSerSalva() {
        assertFalse(completo.copy(categoria = null).podeSalvar)
    }

    @Test
    fun receitaDispensaCategoriaETrocarParaReceitaLimpaACategoria() {
        val receita = completo.comTipo(Tipo.RECEITA)
        assertNull(receita.categoria)
        assertTrue(receita.podeSalvar)
    }

    @Test
    fun descricaoLongaDemaisEhRecusada() {
        val longa = completo.copy(descricao = "a".repeat(61))
        assertEquals("Use até 60 caracteres.", longa.erros().descricao)
        assertFalse(longa.podeSalvar)
    }
}
