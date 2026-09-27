package br.ufrn.fluxo.presentation.importacao

import br.ufrn.fluxo.data.ExtratoNaApi
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportacaoTest {
    private fun transacao(id: String, dia: Int) =
        Transacao(id, "Compra $id", 1_000, LocalDate(2026, 9, dia), Tipo.DESPESA)

    @Test
    fun oQueJaEstaNaListaNaoEntraDeNovo() {
        val existentes = transacoesDeExemplo + transacao("imp-nubank-1", 5)
        val extrato =
            ExtratoNaApi("Nubank", listOf(transacao("imp-nubank-1", 5), transacao("imp-nubank-2", 7)), emptyList())
        val previa = montarPrevia("extrato.ofx", extrato, existentes)
        assertEquals(listOf("imp-nubank-2"), previa.novas.map { it.id })
        assertEquals(1, previa.repetidas)
    }

    @Test
    fun resumoUsaSingularEPlural() {
        val uma =
            EstadoDaImportacao.Previa(
                "a.ofx",
                "Nubank",
                listOf(transacao("a", 1)),
                repetidas = 1,
                ignoradas = listOf("linha de saldo"),
            )
        assertEquals("1 lançamento novo, 1 já estava na lista, 1 linha ignorada (de saldo)", resumoDaPrevia(uma))
        val varias = uma.copy(
            novas = listOf(transacao("a", 1), transacao("b", 2)),
            repetidas = 0,
            ignoradas = emptyList(),
        )
        assertEquals("2 lançamentos novos", resumoDaPrevia(varias))
    }

    @Test
    fun linhasIgnoradasSaoAgrupadasPeloMotivo() {
        val ignoradas =
            listOf("linha de saldo", "linha de saldo", "data inválida: \"32/08/2026\"", "linha de saldo", "valor zero")
        val previa = EstadoDaImportacao.Previa("bb.csv", "Banco do Brasil", emptyList(), 0, ignoradas)
        assertEquals(
            "0 lançamentos novos, 5 linhas ignoradas (3 de saldo, 1 com data inválida, 1 com valor zero)",
            resumoDaPrevia(previa),
        )
    }

    @Test
    fun motivoDesconhecidoNaoMostraOValorDaLinha() {
        assertEquals("com outro problema", motivoCurto("formato estranho: \"R$ 1.234,56\""))
        assertEquals("repetidas no arquivo", motivoCurto("FITID repetido no arquivo"))
    }
}
