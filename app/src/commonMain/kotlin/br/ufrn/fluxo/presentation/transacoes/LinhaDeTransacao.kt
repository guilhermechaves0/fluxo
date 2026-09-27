package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarValor
import br.ufrn.fluxo.presentation.theme.LocalCoresDoFluxo
import br.ufrn.fluxo.presentation.theme.daCategoria

/**
 * Uma transação na lista: ponto na cor da categoria, descrição, categoria e valor.
 *
 * Gasto aparece na cor do texto, sem sinal; entrada aparece em âmbar com "+". Com [aoClicar], a linha
 * inteira vira um botão, e o leitor de tela lê o conteúdo junto e anuncia "abrir detalhes".
 */
@Composable
fun LinhaDeTransacao(transacao: Transacao, modifier: Modifier = Modifier, aoClicar: (() -> Unit)? = null) {
    val entrada = transacao.tipo == Tipo.RECEITA
    val clicavel =
        if (aoClicar == null) {
            Modifier
        } else {
            Modifier.clickable(onClickLabel = "abrir detalhes", role = Role.Button, onClick = aoClicar)
        }
    Row(
        modifier
            .fillMaxWidth()
            .then(clicavel)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 7.dp)
                .size(10.dp)
                .background(LocalCoresDoFluxo.current.daCategoria(transacao.categoria?.id), CircleShape),
        )
        Column(Modifier.weight(1f)) {
            Text(
                transacao.descricao,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                transacao.categoria?.nome ?: if (entrada) "Entrada" else "Sem categoria",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            if (entrada) "+ ${formatarValor(transacao.valorCentavos)}" else formatarValor(transacao.valorCentavos),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (entrada) FontWeight.Medium else FontWeight.Normal,
            color = if (entrada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
