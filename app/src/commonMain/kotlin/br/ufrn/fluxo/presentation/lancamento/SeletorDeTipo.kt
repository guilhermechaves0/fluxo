package br.ufrn.fluxo.presentation.lancamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.presentation.transacoes.rotulo

/** Escolha entre despesa e receita. Recebe o tipo atual e avisa quando outro é escolhido. */
@Composable
fun SeletorDeTipo(selecionado: Tipo, aoSelecionar: (Tipo) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Tipo.DESPESA, Tipo.RECEITA).forEach { tipo ->
            FilterChip(
                selected = tipo == selecionado,
                onClick = { aoSelecionar(tipo) },
                label = { Text(tipo.rotulo) },
            )
        }
    }
}
