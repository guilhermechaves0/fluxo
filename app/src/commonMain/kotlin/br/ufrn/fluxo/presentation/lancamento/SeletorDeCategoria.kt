package br.ufrn.fluxo.presentation.lancamento

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Categoria

/**
 * Lista de categorias em chips, com rolagem horizontal quando não cabem na largura.
 * Sem categoria escolhida, mostra a dica de que ela é obrigatória.
 */
@Composable
fun SeletorDeCategoria(
    categorias: List<Categoria>,
    selecionada: Categoria?,
    aoSelecionar: (Categoria) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Categoria", style = MaterialTheme.typography.labelLarge)
        Row(
            Modifier.selectableGroup().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categorias.forEach { categoria ->
                FilterChip(
                    selected = categoria == selecionada,
                    onClick = { aoSelecionar(categoria) },
                    label = { Text(categoria.nome) },
                )
            }
        }
        if (selecionada == null) {
            Text(
                "Obrigatória para despesas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
