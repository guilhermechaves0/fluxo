package br.ufrn.fluxo.presentation.importacao

import android.net.Uri
import android.provider.OpenableColumns
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
            val arquivo =
                uri?.let {
                    val resolvedor = contexto.contentResolver
                    val nome =
                        resolvedor.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) cursor.getString(0) else null
                        } ?: "extrato"
                    resolvedor.openInputStream(it)?.use { entrada -> ArquivoEscolhido(nome, entrada.readBytes()) }
                }
            aoEscolher(arquivo)
        }
    return { seletor.launch(TIPOS_ACEITOS) }
}
