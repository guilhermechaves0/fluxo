package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.doMes
import br.ufrn.fluxo.dominio.formatarMes
import br.ufrn.fluxo.dominio.formatarReais
import br.ufrn.fluxo.dominio.porTipo
import br.ufrn.fluxo.dominio.saldo

/**
 * Tela das transações de um mês, com o saldo desse mês e filtro por tipo. As mais recentes
 * aparecem primeiro.
 *
 * A tela não guarda estado: recebe a lista inteira, o mês e o filtro e avisa pelos callbacks quando o
 * mês ou o filtro muda, quando uma transação é tocada, quando o botão + é tocado e quando a importação de extrato
 * é pedida. Quem navega é o NavHost.
 * Por isso os previews montam a tela cheia e a vazia só passando parâmetros.
 */
@Composable
fun TelaTransacoes(
    transacoes: List<Transacao>,
    mes: Mes,
    filtro: FiltroTransacoes,
    aoTrocarMes: (Mes) -> Unit,
    aoTrocarFiltro: (FiltroTransacoes) -> Unit,
    aoAbrir: (id: String) -> Unit,
    aoAdicionar: () -> Unit,
    modifier: Modifier = Modifier,
    aoImportar: () -> Unit = {},
) {
    val doMes = remember(transacoes, mes) { transacoes.doMes(mes) }
    val visiveis = remember(doMes, filtro) { doMes.porTipo(filtro.tipo).sortedByDescending { it.data } }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = aoAdicionar,
                modifier = Modifier.semantics { contentDescription = "Adicionar transação" },
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics {})
            }
        },
    ) { espacamento ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(espacamento)
                .padding(horizontal = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Transações",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = aoImportar) { Text("Importar extrato") }
            }
            SeletorDeMes(mes = mes, aoTrocar = aoTrocarMes)
            Text(
                "Saldo do mês: ${formatarReais(doMes.saldo())}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FiltrosDeTipo(selecionado = filtro, aoSelecionar = aoTrocarFiltro)
            Spacer(Modifier.height(12.dp))
            if (visiveis.isEmpty()) {
                EstadoVazio(filtro, mes)
            } else {
                // O respiro no fim deixa a última transação acima do botão +, que flutua sobre a lista.
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(visiveis, key = { it.id }) { transacao ->
                        CartaoTransacao(transacao, aoClicar = { aoAbrir(transacao.id) })
                    }
                }
            }
        }
    }
}

/** Mensagem mostrada quando nenhuma transação atende ao filtro. */
@Composable
private fun EstadoVazio(filtro: FiltroTransacoes, mes: Mes, modifier: Modifier = Modifier) {
    val nome = formatarMes(mes)
    val mensagem =
        when (filtro) {
            FiltroTransacoes.TODAS -> "Nenhuma transação em $nome. Toque em + para lançar ou importe o extrato."
            FiltroTransacoes.RECEITAS -> "Nenhuma receita em $nome."
            FiltroTransacoes.DESPESAS -> "Nenhuma despesa em $nome."
        }
    Text(
        mensagem,
        modifier = modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
