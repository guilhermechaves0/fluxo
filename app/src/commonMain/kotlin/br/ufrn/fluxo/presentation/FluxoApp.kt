package br.ufrn.fluxo.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.ufrn.fluxo.presentation.importacao.ArquivoEscolhido
import br.ufrn.fluxo.presentation.navegacao.FluxoNavegacao
import br.ufrn.fluxo.presentation.theme.FluxoTema

/**
 * Raiz do app no desktop e no Android. Main.kt e MainActivity só chamam esta função.
 *
 * Aplica o tema e afasta o conteúdo das barras do sistema. O estado e a navegação ficam em
 * [FluxoNavegacao]. [arquivoCompartilhado] é o extrato que outro app mandou pelo "Compartilhar".
 */
@Composable
fun FluxoApp(arquivoCompartilhado: ArquivoEscolhido? = null) {
    FluxoTema {
        Surface(Modifier.fillMaxSize()) {
            FluxoNavegacao(modifier = Modifier.safeDrawingPadding(), arquivoCompartilhado = arquivoCompartilhado)
        }
    }
}
