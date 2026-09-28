package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.formatarMes

/**
 * Mês mostrado na lista, com botões para o anterior e o seguinte. Recebe o mês e avisa quando
 * outro é escolhido. O leitor de tela anuncia o mês novo a cada troca.
 */
@Composable
fun SeletorDeMes(mes: Mes, aoTrocar: (Mes) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { aoTrocar(mes.anterior()) },
            modifier = Modifier.semantics { contentDescription = "Mês anterior" },
        ) {
            Text("‹", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
        }
        Text(
            formatarMes(mes).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            // O mês é o título da tela: o leitor de tela anuncia como cabeçalho e avisa a cada troca.
            modifier =
            Modifier.semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            },
        )
        IconButton(
            onClick = { aoTrocar(mes.seguinte()) },
            modifier = Modifier.semantics { contentDescription = "Próximo mês" },
        ) {
            Text("›", style = MaterialTheme.typography.titleLarge, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}
