package br.ufrn.fluxo.presentation.importacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.presentation.transacoes.LinhaDeTransacao

/**
 * Importação de extrato em três passos: escolher o arquivo, conferir a prévia e confirmar.
 *
 * A tela não guarda estado: desenha o [estado] recebido e avisa pelos callbacks. Nada entra na
 * lista antes de [aoConfirmar]. Mudanças de passo são anunciadas pelo leitor de tela.
 */
@Composable
fun TelaImportacao(
    estado: EstadoDaImportacao,
    aoEscolherArquivo: () -> Unit,
    aoConfirmar: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = aoVoltar) { Text("Voltar") }
        Text(
            "Importar extrato",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        when (estado) {
            EstadoDaImportacao.Inicial -> PassoInicial(aoEscolherArquivo)
            is EstadoDaImportacao.Lendo -> PassoLendo(estado)
            is EstadoDaImportacao.Previa -> PassoPrevia(estado, aoConfirmar, aoEscolherArquivo)
            is EstadoDaImportacao.Falha -> PassoFalha(estado, aoEscolherArquivo)
        }
    }
}

@Composable
private fun PassoInicial(aoEscolherArquivo: () -> Unit) {
    Text(
        "Exporte o extrato no app ou no site do banco, em OFX ou CSV, e escolha o arquivo aqui. O Fluxo " +
            "reconhece a conta e a fatura do cartão do Nubank, do C6 Bank e do Banco do Brasil.",
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        "Nada entra na sua lista antes de você conferir a prévia e confirmar.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = aoEscolherArquivo, modifier = Modifier.fillMaxWidth()) { Text("Escolher arquivo") }
}

@Composable
private fun PassoLendo(estado: EstadoDaImportacao.Lendo) {
    Row(
        Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text("Lendo ${estado.nomeDoArquivo}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ColumnScope.PassoPrevia(
    estado: EstadoDaImportacao.Previa,
    aoConfirmar: () -> Unit,
    aoEscolherArquivo: () -> Unit,
) {
    Column(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        Text("${estado.origem} · ${estado.nomeDoArquivo}", style = MaterialTheme.typography.titleMedium)
        Text(
            resumoDaPrevia(estado),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Button(onClick = aoConfirmar, enabled = estado.novas.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
        Text(if (estado.novas.isEmpty()) "Nada novo para importar" else "Importar ${quantos(estado.novas.size)}")
    }
    OutlinedButton(onClick = aoEscolherArquivo, modifier = Modifier.fillMaxWidth()) { Text("Escolher outro arquivo") }
    LazyColumn(Modifier.weight(1f)) {
        items(estado.novas, key = { it.id }) { transacao -> LinhaDeTransacao(transacao) }
    }
}

@Composable
private fun PassoFalha(estado: EstadoDaImportacao.Falha, aoEscolherArquivo: () -> Unit) {
    Text(
        estado.mensagem,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
    Button(onClick = aoEscolherArquivo, modifier = Modifier.fillMaxWidth()) { Text("Escolher outro arquivo") }
}

/** "3 lançamentos novos, 2 já estavam na lista, 6 linhas ignoradas (5 de saldo, 1 com data inválida)". */
fun resumoDaPrevia(previa: EstadoDaImportacao.Previa): String = buildList {
    add(if (previa.novas.size == 1) "1 lançamento novo" else "${previa.novas.size} lançamentos novos")
    if (previa.repetidas > 0) add("${previa.repetidas} já estava${if (previa.repetidas == 1) "" else "m"} na lista")
    if (previa.ignoradas.isNotEmpty()) add(resumoDasIgnoradas(previa.ignoradas))
}.joinToString(", ")

private fun resumoDasIgnoradas(motivos: List<String>): String {
    val total = if (motivos.size == 1) "1 linha ignorada" else "${motivos.size} linhas ignoradas"
    val grupos =
        motivos
            .groupingBy { motivoCurto(it) }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    val detalhe =
        if (grupos.size == 1) {
            grupos.single().key
        } else {
            grupos.joinToString(", ") { "${it.value} ${it.key}" }
        }
    return "$total ($detalhe)"
}

/** Traduz o motivo que o importador devolve para uma frase curta. O que vem depois de ":" é o valor da linha. */
fun motivoCurto(motivo: String): String {
    val chave = motivo.substringBefore(":").trim().lowercase()
    return when {
        chave == "linha de saldo" -> "de saldo"
        chave == "valor zero" -> "com valor zero"
        chave.contains("repetido") -> "repetidas no arquivo"
        chave.startsWith("data inválida") -> "com data inválida"
        chave.startsWith("valor inválido") -> "com valor inválido"
        chave.startsWith("campo obrigatório ausente") -> "sem data ou valor"
        else -> "com outro problema"
    }
}

private fun quantos(n: Int) = if (n == 1) "1 lançamento" else "$n lançamentos"
