package br.ufrn.fluxo.presentation.transacoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.doMes
import br.ufrn.fluxo.dominio.formatarMes
import br.ufrn.fluxo.dominio.porTipo
import kotlinx.datetime.LocalDate

private val LARGURA_DO_PAINEL = 360.dp
private val FOLHA = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/**
 * Tela das transações de um mês: a frase com o que saiu e o que entrou, a barra de gastos por categoria
 * e a lista agrupada por dia.
 *
 * Na janela estreita, tudo fica numa coluna, e a lista sobe como uma folha por cima do fundo. Na janela
 * larga ([largo]), o resumo do mês vira um painel à esquerda, com a legenda completa, e os dias ficam à
 * direita. A tela não guarda estado: recebe as transações, o mês e os filtros e avisa pelos callbacks.
 */
@Composable
fun TelaTransacoes(
    transacoes: List<Transacao>,
    mes: Mes,
    filtro: FiltroTransacoes,
    categoria: String?,
    hoje: LocalDate,
    aoTrocarMes: (Mes) -> Unit,
    aoTrocarFiltro: (FiltroTransacoes) -> Unit,
    aoTrocarCategoria: (String?) -> Unit,
    aoAbrir: (id: String) -> Unit,
    aoAdicionar: () -> Unit,
    modifier: Modifier = Modifier,
    largo: Boolean = false,
    aoImportar: () -> Unit = {},
) {
    val doMes = remember(transacoes, mes) { transacoes.doMes(mes) }
    val fatias = remember(doMes) { doMes.fatiasDeGasto() }
    val visiveis = remember(doMes, filtro, categoria) { doMes.porTipo(filtro.tipo).daCategoria(categoria) }
    val gasto = doMes.filter { it.tipo == Tipo.DESPESA }.sumOf { it.valorCentavos }
    val entrada = doMes.filter { it.tipo == Tipo.RECEITA }.sumOf { it.valorCentavos }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { BotaoDeLancar(aoAdicionar) },
    ) { espacamento ->
        val resumo: @Composable ColumnScope.() -> Unit = {
            CabecalhoDoMes(mes, aoTrocarMes, aoImportar, comImportar = !largo)
            Spacer(Modifier.height(12.dp))
            FraseDoMes(gasto, entrada, mes)
            if (fatias.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                BarraDeCategorias(fatias, categoria)
            }
        }
        val lista: @Composable () -> Unit = {
            ListaDoMes(visiveis, filtro, categoria, mes, hoje, aoTrocarFiltro, aoAbrir)
        }
        if (largo) {
            Row(Modifier.fillMaxSize().padding(espacamento)) {
                Column(
                    Modifier
                        .width(LARGURA_DO_PAINEL)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                ) {
                    resumo()
                    Spacer(Modifier.height(20.dp))
                    PainelDeCategorias(fatias, categoria, aoTrocarCategoria)
                    // No painel estreito de 360 dp, o botão ao lado do mês espremeria o seletor.
                    TextButton(onClick = aoImportar, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Importar extrato")
                    }
                }
                Surface(
                    Modifier.weight(1f).fillMaxHeight().padding(top = 16.dp, end = 16.dp),
                    shape = FOLHA,
                    color = MaterialTheme.colorScheme.surface,
                ) { lista() }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(espacamento)) {
                Column(Modifier.padding(horizontal = 20.dp)) { resumo() }
                if (fatias.isNotEmpty()) {
                    FiltroDeCategorias(
                        fatias,
                        categoria,
                        aoTrocarCategoria,
                        Modifier.padding(top = 12.dp),
                        margem = PaddingValues(horizontal = 20.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Surface(Modifier.fillMaxSize(), shape = FOLHA, color = MaterialTheme.colorScheme.surface) { lista() }
            }
        }
    }
}

@Composable
private fun CabecalhoDoMes(mes: Mes, aoTrocarMes: (Mes) -> Unit, aoImportar: () -> Unit, comImportar: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SeletorDeMes(mes = mes, aoTrocar = aoTrocarMes, modifier = Modifier.weight(1f))
        if (comImportar) {
            TextButton(onClick = aoImportar) { Text("Importar extrato") }
        }
    }
}

@Composable
private fun ListaDoMes(
    visiveis: List<Transacao>,
    filtro: FiltroTransacoes,
    categoria: String?,
    mes: Mes,
    hoje: LocalDate,
    aoTrocarFiltro: (FiltroTransacoes) -> Unit,
    aoAbrir: (id: String) -> Unit,
) {
    Column {
        FiltrosDeTipo(
            selecionado = filtro,
            aoSelecionar = aoTrocarFiltro,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp),
        )
        if (visiveis.isEmpty()) {
            EstadoVazio(filtro, categoria, mes)
        } else {
            // O respiro no fim deixa a última transação acima do botão +, que flutua sobre a lista.
            LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                transacoesPorDia(visiveis, hoje, aoAbrir)
            }
        }
    }
}

@Composable
private fun BotaoDeLancar(aoAdicionar: () -> Unit) {
    FloatingActionButton(
        onClick = aoAdicionar,
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.semantics { contentDescription = "Adicionar transação" },
    ) {
        Text("+", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clearAndSetSemantics {})
    }
}

/** Mensagem quando nada no mês atende aos filtros: diz o que fazer em vez de só lamentar. */
@Composable
private fun EstadoVazio(filtro: FiltroTransacoes, categoria: String?, mes: Mes, modifier: Modifier = Modifier) {
    val nome = formatarMes(mes)
    val mensagem =
        when {
            categoria != null -> "Nada nessa categoria em $nome com esse filtro."
            filtro == FiltroTransacoes.TODAS -> "Nenhuma transação em $nome. Lance com o + ou importe o extrato."
            filtro == FiltroTransacoes.RECEITAS -> "Nenhuma entrada em $nome."
            else -> "Nenhum gasto em $nome."
        }
    Text(
        mensagem,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 24.dp),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
