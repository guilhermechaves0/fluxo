package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.formatarValor
import br.ufrn.fluxo.presentation.theme.LocalCoresDoFluxo
import br.ufrn.fluxo.presentation.theme.daCategoria

/** Ponto na cor da categoria. É decorativo: o nome ao lado diz a categoria. */
@Composable
fun PontoDaCategoria(id: String?, modifier: Modifier = Modifier) {
    val cor = LocalCoresDoFluxo.current.daCategoria(id?.takeIf { it != ID_SEM_CATEGORIA })
    Box(modifier.size(10.dp).background(cor, CircleShape))
}

/**
 * Legenda da barra em chips com rolagem horizontal, para a tela estreita. Tocar numa categoria filtra
 * a lista por ela; tocar de novo volta a mostrar todas.
 */
@Composable
fun FiltroDeCategorias(
    fatias: List<FatiaDoMes>,
    selecionada: String?,
    aoSelecionar: (String?) -> Unit,
    modifier: Modifier = Modifier,
    margem: PaddingValues = PaddingValues(0.dp),
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectableGroup()
            .horizontalScroll(rememberScrollState())
            .padding(margem),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        fatias.forEach { fatia ->
            FilterChip(
                selected = fatia.id == selecionada,
                onClick = { aoSelecionar(if (fatia.id == selecionada) null else fatia.id) },
                label = { Text("${fatia.nome} ${fatia.percentual}") },
                leadingIcon = { PontoDaCategoria(fatia.id) },
            )
        }
    }
}

/**
 * Legenda completa para o painel da tela larga: cada categoria numa linha, com o valor e o peso no mês.
 * A linha inteira é selecionável e filtra a lista ao lado.
 */
@Composable
fun PainelDeCategorias(
    fatias: List<FatiaDoMes>,
    selecionada: String?,
    aoSelecionar: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fundoEscolhido = MaterialTheme.colorScheme.secondaryContainer
    val fundo = MaterialTheme.colorScheme.background
    Column(modifier.selectableGroup()) {
        fatias.forEach { fatia ->
            val escolhida = fatia.id == selecionada
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (escolhida) fundoEscolhido else fundo)
                    .selectable(
                        selected = escolhida,
                        role = Role.RadioButton,
                        onClick = { aoSelecionar(if (escolhida) null else fatia.id) },
                    ).padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PontoDaCategoria(fatia.id)
                Text(fatia.nome, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(formatarValor(fatia.centavos), style = MaterialTheme.typography.bodyLarge)
                Text(
                    fatia.percentual,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
