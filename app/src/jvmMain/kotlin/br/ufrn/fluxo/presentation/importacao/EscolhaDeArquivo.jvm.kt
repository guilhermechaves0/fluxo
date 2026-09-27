package br.ufrn.fluxo.presentation.importacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

/** No desktop, a janela de arquivos do sistema operacional. */
@Composable
actual fun rememberEscolhaDeArquivo(aoEscolher: (ArquivoEscolhido?) -> Unit): () -> Unit {
    val aoEscolherAtual = rememberUpdatedState(aoEscolher)
    return remember {
        {
            val janela = FileDialog(null as Frame?, "Escolha o extrato (OFX ou CSV)", FileDialog.LOAD)
            janela.isVisible = true
            val arquivo = janela.file?.let { File(janela.directory, it) }
            aoEscolherAtual.value(arquivo?.let { ArquivoEscolhido(it.name, it.readBytes()) })
        }
    }
}
