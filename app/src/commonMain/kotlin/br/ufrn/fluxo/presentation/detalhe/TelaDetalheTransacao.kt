package br.ufrn.fluxo.presentation.detalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarData
import br.ufrn.fluxo.dominio.formatarReais
import br.ufrn.fluxo.presentation.transacoes.rotulo

/**
 * Detalhe de uma transação, aberto pela lista ou pelo deep link fluxo://transacao/{id}.
 *
 * Recebe `null` quando o id não existe, o que acontece com um link antigo, e mostra um aviso no
 * lugar dos dados.
 */
@Composable
fun TelaDetalheTransacao(transacao: Transacao?, aoVoltar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = aoVoltar) { Text("Voltar") }
        if (transacao == null) {
            Text(
                "Transação não encontrada",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                "O link pode estar desatualizado. Volte para a lista e escolha a transação.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val corDoValor =
            when (transacao.tipo) {
                Tipo.RECEITA -> MaterialTheme.colorScheme.primary
                Tipo.DESPESA -> MaterialTheme.colorScheme.error
            }
        Text(
            transacao.descricao,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            formatarReais(transacao.valorComSinal),
            style = MaterialTheme.typography.displaySmall,
            color = corDoValor,
        )
        HorizontalDivider()
        LinhaDeDetalhe("Tipo", transacao.tipo.rotulo)
        LinhaDeDetalhe("Categoria", transacao.categoria?.nome ?: "Sem categoria")
        LinhaDeDetalhe("Data", formatarData(transacao.data))
    }
}

/** Rótulo e valor lidos juntos pelo leitor de tela, como "Categoria, Moradia". */
@Composable
private fun LinhaDeDetalhe(rotulo: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(rotulo, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
