package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.presentation.theme.FluxoTema
import kotlinx.datetime.LocalDate

// Previews da tela cheia (clara e escura), vazia e larga, e da linha de transação. Aparecem no
// Android Studio ou no IntelliJ com o plugin Kotlin Multiplatform.

private val setembro = Mes(2026, 9)
private val hoje = LocalDate(2026, 9, 10)

@Composable
private fun Tela(escuro: Boolean = false, vazia: Boolean = false, largo: Boolean = false) {
    FluxoTema(escuro = escuro) {
        TelaTransacoes(
            transacoes = if (vazia) emptyList() else transacoesDeExemplo,
            mes = setembro,
            filtro = FiltroTransacoes.TODAS,
            categoria = null,
            hoje = hoje,
            aoTrocarMes = {},
            aoTrocarFiltro = {},
            aoTrocarCategoria = {},
            aoAbrir = {},
            aoAdicionar = {},
            largo = largo,
        )
    }
}

@Preview
@Composable
fun TelaTransacoesCheiaPreview() = Tela()

@Preview
@Composable
fun TelaTransacoesEscuraPreview() = Tela(escuro = true)

@Preview
@Composable
fun TelaTransacoesVaziaPreview() = Tela(vazia = true)

@Preview(widthDp = 900, heightDp = 600)
@Composable
fun TelaTransacoesLargaPreview() = Tela(largo = true)

@Preview
@Composable
fun LinhaDeTransacaoPreview() {
    FluxoTema {
        LinhaDeTransacao(transacoesDeExemplo[1])
    }
}
