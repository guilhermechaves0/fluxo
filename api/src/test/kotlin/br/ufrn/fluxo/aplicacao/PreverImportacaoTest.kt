package br.ufrn.fluxo.aplicacao

import br.ufrn.fluxo.dominio.Tipo
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PreverImportacaoTest {
    private fun prever(vararg lancamentos: LancamentoLido, origem: String = "Banco do Brasil") = PreverImportacao(
        object : ImportadorDeExtratos {
            override suspend fun ler(conteudo: ByteArray) =
                ExtratoLido("OFX", origem, lancamentos.toList(), emptyList())
        },
    )

    private val dia = LocalDate(2026, 9, 10)

    @Test
    fun oIdentificadorLevaOBancoParaNaoMisturarExtratosDeBancosDiferentes() = runTest {
        val previa = prever(LancamentoLido("20260910001", dia, 8_935, "BOLETO", Tipo.DESPESA))(byteArrayOf(1))
        assertEquals("imp-bancodobrasil-20260910001", previa.transacoes.single().id)
    }

    @Test
    fun descricaoVaziaViraSemDescricaoEValorZeroEhIgnorado() = runTest {
        val previa =
            prever(
                LancamentoLido("a", dia, 100, " ", Tipo.RECEITA),
                LancamentoLido("b", dia, 0, "TARIFA ZERADA", Tipo.DESPESA),
            )(byteArrayOf(1))
        assertEquals("Sem descrição", previa.transacoes.single().descricao)
        assertEquals("valor zero", previa.ignorados.single().motivo)
    }

    @Test
    fun arquivoVazioOuGrandeDemaisEhRecusadoAntesDeChamarOImportador() = runTest {
        assertFailsWith<IllegalArgumentException> { prever()(ByteArray(0)) }
        assertFailsWith<ArquivoGrandeDemais> { prever()(ByteArray(5 * 1024 * 1024 + 1)) }
    }

    @Test
    fun aCategoriaDoBancoVemComIdSemAcento() = runTest {
        val lido = LancamentoLido("x", dia, 4_590, "PAGAMENTO", Tipo.RECEITA, categoria = "Pagamentos/Créditos")
        val categoria = prever(lido)(byteArrayOf(1)).transacoes.single().categoria
        assertEquals("pagamentos-creditos", categoria?.id)
        assertEquals("Pagamentos/Créditos", categoria?.nome)
    }
}
