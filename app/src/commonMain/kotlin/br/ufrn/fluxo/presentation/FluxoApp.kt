package br.ufrn.fluxo.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import br.ufrn.fluxo.data.novaTransacaoDeExemplo
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.presentation.theme.FluxoTema
import br.ufrn.fluxo.presentation.transacoes.FiltroTransacoes
import br.ufrn.fluxo.presentation.transacoes.TelaTransacoes

/**
 * Raiz do app no desktop e no Android.
 *
 * Guarda o filtro e a lista de transações, passa os valores para a tela e atualiza o estado
 * quando a tela avisa uma ação. Main.kt e MainActivity só chamam esta função. Na Sprint 2 o
 * estado vai para um ViewModel.
 */
@Composable
fun FluxoApp() {
    var filtro by remember { mutableStateOf(FiltroTransacoes.TODAS) }
    var transacoes by remember { mutableStateOf(transacoesDeExemplo) }

    FluxoTema {
        Surface(Modifier.fillMaxSize()) {
            TelaTransacoes(
                transacoes = transacoes,
                filtro = filtro,
                aoTrocarFiltro = { filtro = it },
                aoAdicionar = { transacoes = transacoes + novaTransacaoDeExemplo(transacoes.size) },
            )
        }
    }
}
