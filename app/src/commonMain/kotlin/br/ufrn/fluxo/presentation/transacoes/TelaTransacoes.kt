package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarReais
import br.ufrn.fluxo.dominio.porTipo
import br.ufrn.fluxo.dominio.saldo

/**
 * Tela das transações do mês, com saldo e filtro por tipo.
 *
 * A tela não guarda estado: recebe a lista e o filtro e avisa o FluxoApp pelos callbacks.
 * Por isso os previews montam a tela cheia e a vazia só passando parâmetros.
 */
@Composable
fun TelaTransacoes(
    transacoes: List<Transacao>,
    filtro: FiltroTransacoes,
    aoTrocarFiltro: (FiltroTransacoes) -> Unit,
    aoAdicionar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visiveis = remember(transacoes, filtro) { transacoes.porTipo(filtro.tipo) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = aoAdicionar,
                modifier = Modifier.semantics { contentDescription = "Adicionar transação" },
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        },
    ) { espacamento ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(espacamento)
                .padding(horizontal = 16.dp),
        ) {
            Text("Transações", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Saldo do mês: ${formatarReais(transacoes.saldo())}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FiltrosDeTipo(selecionado = filtro, aoSelecionar = aoTrocarFiltro)
            Spacer(Modifier.height(12.dp))
            if (visiveis.isEmpty()) {
                EstadoVazio(filtro)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visiveis, key = { it.id }) { transacao ->
                        CartaoTransacao(transacao)
                    }
                }
            }
        }
    }
}

/** Mensagem mostrada quando nenhuma transação atende ao filtro. */
@Composable
private fun EstadoVazio(filtro: FiltroTransacoes, modifier: Modifier = Modifier) {
    val mensagem =
        when (filtro) {
            FiltroTransacoes.TODAS -> "Nenhuma transação ainda. Toque em + para lançar a primeira."
            FiltroTransacoes.RECEITAS -> "Nenhuma receita neste mês."
            FiltroTransacoes.DESPESAS -> "Nenhuma despesa neste mês."
        }
    Text(
        mensagem,
        modifier = modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
