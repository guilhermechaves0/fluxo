package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.presentation.theme.LocalCoresDoFluxo
import br.ufrn.fluxo.presentation.theme.daCategoria

private const val PESO_MINIMO = 0.012f
private const val OPACIDADE_APAGADA = 0.3f

/**
 * Para onde foi o dinheiro: uma faixa dividida pelo peso de cada categoria no gasto do mês. É o
 * elemento marcante do app. Com uma categoria escolhida no filtro, as outras ficam apagadas. O leitor
 * de tela lê a divisão por extenso, porque a cor sozinha não diz nada a quem não enxerga.
 */
@Composable
fun BarraDeCategorias(fatias: List<FatiaDoMes>, selecionada: String?, modifier: Modifier = Modifier) {
    val cores = LocalCoresDoFluxo.current
    val leitura = "Gastos por categoria: " + fatias.joinToString(", ") { "${it.nome} ${it.percentual}" }
    Row(
        modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .semantics { contentDescription = leitura },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        fatias.forEach { fatia ->
            Box(
                Modifier
                    .weight(maxOf(fatia.fracao, PESO_MINIMO))
                    .fillMaxHeight()
                    .alpha(if (selecionada == null || selecionada == fatia.id) 1f else OPACIDADE_APAGADA)
                    .background(cores.daCategoria(fatia.categoria?.id)),
            )
        }
    }
}
