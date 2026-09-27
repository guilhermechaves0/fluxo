package br.ufrn.fluxo.presentation.importacao

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// O OFX não tem um tipo MIME padrão e cada banco manda de um jeito, então o seletor aceita tudo.
private val TIPOS_ACEITOS = arrayOf("*/*")

/** Seletor de documentos do Android (Storage Access Framework): não pede permissão de armazenamento. */
@Composable
actual fun rememberEscolhaDeArquivo(aoEscolher: (ArquivoEscolhido?) -> Unit): () -> Unit {
    val contexto = LocalContext.current
    val seletor =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            aoEscolher(uri?.let { lerArquivo(contexto, it) })
        }
    return { seletor.launch(TIPOS_ACEITOS) }
}
