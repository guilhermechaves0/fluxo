package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarValor
import kotlinx.datetime.LocalDate

/**
 * Transações agrupadas por dia, com o total do dia no cabeçalho quando o dia tem mais de uma. O
 * cabeçalho é marcado como título, para o leitor de tela pular de um dia para o outro. Um fio fino
 * separa as transações do mesmo dia.
 */
fun LazyListScope.transacoesPorDia(transacoes: List<Transacao>, hoje: LocalDate, aoAbrir: (id: String) -> Unit) {
    transacoes.porDia().forEach { (dia, doDia) ->
        item(key = "dia-$dia") {
            val saldo = doDia.sumOf { it.valorComSinal }
            val corDoTotal =
                if (saldo > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)
                    .semantics(mergeDescendants = true) { heading() },
            ) {
                Text(
                    rotuloDoDia(dia, hoje),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                // Com uma transação só, o total do dia repetiria o valor da linha logo abaixo.
                if (doDia.size > 1) {
                    Text(
                        if (saldo > 0) "+ ${formatarValor(saldo)}" else formatarValor(saldo),
                        style = MaterialTheme.typography.labelLarge,
                        color = corDoTotal,
                    )
                }
            }
        }
        doDia.forEachIndexed { indice, transacao ->
            item(key = transacao.id) {
                if (indice > 0) {
                    HorizontalDivider(
                        Modifier.padding(start = 42.dp, end = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
                LinhaDeTransacao(transacao, aoClicar = { aoAbrir(transacao.id) })
            }
        }
    }
}
