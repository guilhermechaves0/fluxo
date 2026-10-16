package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Chips de filtro por tipo. Recebem o filtro selecionado e avisam quando outro é escolhido. */
@Composable
fun FiltrosDeTipo(
    selecionado: FiltroTransacoes,
    aoSelecionar: (FiltroTransacoes) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FiltroTransacoes.entries.forEach { filtro ->
            val escolhido = filtro == selecionado
            FilterChip(
                selected = escolhido,
                onClick = { aoSelecionar(filtro) },
                label = { Text(filtro.rotulo) },
                border =
                FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = escolhido,
                    borderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}
