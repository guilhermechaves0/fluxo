package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.formatarMes
import br.ufrn.fluxo.dominio.formatarReais

/**
 * O resumo do mês numa frase: "Saíram R$ 1.882,15 e entraram R$ 5.300,00". O que entrou fica em
 * âmbar. A frase no lugar de um número grande com rótulo evita o painel genérico e se lê em voz alta
 * do mesmo jeito que aparece.
 */
@Composable
fun FraseDoMes(gasto: Long, entrada: Long, mes: Mes, modifier: Modifier = Modifier) {
    val destaque = SpanStyle(fontWeight = FontWeight.Medium)
    val corDaEntrada = MaterialTheme.colorScheme.primary
    val frase =
        buildAnnotatedString {
            when {
                gasto == 0L && entrada == 0L -> append("Nada entrou nem saiu em ${formatarMes(mes)}.")

                entrada == 0L -> {
                    append("Saíram ")
                    withStyle(destaque) { append(formatarReais(gasto)) }
                }

                gasto == 0L -> {
                    append("Entraram ")
                    withStyle(destaque.copy(color = corDaEntrada)) { append(formatarReais(entrada)) }
                }

                else -> {
                    append("Saíram ")
                    withStyle(destaque) { append(formatarReais(gasto)) }
                    append("\ne entraram ")
                    withStyle(destaque.copy(color = corDaEntrada)) { append(formatarReais(entrada)) }
                }
            }
        }
    Text(frase, modifier = modifier, style = MaterialTheme.typography.headlineSmall)
}
