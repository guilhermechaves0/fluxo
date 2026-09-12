package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarReais

/**
 * Cartão com descrição, categoria, data e valor de uma transação.
 *
 * Recebe só a transação e não guarda estado, então pode ser reaproveitado em outras telas,
 * como o detalhe de uma categoria.
 */
@Composable
fun CartaoTransacao(transacao: Transacao, modifier: Modifier = Modifier) {
    val corDoValor =
        when (transacao.tipo) {
            Tipo.RECEITA -> MaterialTheme.colorScheme.primary
            Tipo.DESPESA -> MaterialTheme.colorScheme.error
        }
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    transacao.descricao,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${transacao.categoria?.nome ?: "Sem categoria"} · ${transacao.data}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatarReais(transacao.valorComSinal),
                style = MaterialTheme.typography.titleMedium,
                color = corDoValor,
            )
        }
    }
}
