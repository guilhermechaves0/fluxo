package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.presentation.theme.FluxoTema

// Previews da tela com lista cheia e vazia e do cartão de transação. Aparecem no Android Studio
// ou no IntelliJ com o plugin Kotlin Multiplatform. Para ver no desktop, use ./gradlew :app:hotRunJvm.

@Preview
@Composable
fun TelaTransacoesCheiaPreview() {
    FluxoTema {
        TelaTransacoes(
            transacoes = transacoesDeExemplo,
            mes = Mes(2026, 9),
            filtro = FiltroTransacoes.TODAS,
            aoTrocarMes = {},
            aoTrocarFiltro = {},
            aoAbrir = {},
            aoAdicionar = {},
        )
    }
}

@Preview
@Composable
fun TelaTransacoesVaziaPreview() {
    FluxoTema(escuro = true) {
        TelaTransacoes(
            transacoes = emptyList(),
            mes = Mes(2026, 9),
            filtro = FiltroTransacoes.TODAS,
            aoTrocarMes = {},
            aoTrocarFiltro = {},
            aoAbrir = {},
            aoAdicionar = {},
        )
    }
}

@Preview
@Composable
fun CartaoTransacaoPreview() {
    FluxoTema {
        CartaoTransacao(transacoesDeExemplo.first())
    }
}
