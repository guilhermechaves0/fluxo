package br.ufrn.fluxo.presentation.importacao

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.IntentCompat
import java.io.IOException

private const val NOME_PADRAO = "extrato"

/** Lê o arquivo de um content:// (seletor do sistema ou compartilhamento de outro app). */
fun lerArquivo(contexto: Context, uri: Uri): ArquivoEscolhido? {
    val resolvedor = contexto.contentResolver
    return try {
        val nome =
            resolvedor.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: NOME_PADRAO
        resolvedor.openInputStream(uri)?.use { entrada -> ArquivoEscolhido(nome, entrada.readBytes()) }
    } catch (e: IOException) {
        Log.w("Fluxo", "não consegui ler o arquivo compartilhado", e)
        null
    } catch (e: SecurityException) {
        Log.w("Fluxo", "o outro app não deu permissão de leitura", e)
        null
    }
}

/**
 * Arquivo que outro app mandou pelo "Compartilhar" do Android, como o CSV do extrato no app do
 * banco. Alguns apps mandam o arquivo (EXTRA_STREAM); outros, o conteúdo como texto (EXTRA_TEXT).
 */
fun arquivoCompartilhado(contexto: Context, intent: Intent?): ArquivoEscolhido? {
    if (intent?.action != Intent.ACTION_SEND) return null
    val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
    val texto = intent.getStringExtra(Intent.EXTRA_TEXT)
    return when {
        uri != null -> lerArquivo(contexto, uri)
        texto != null -> ArquivoEscolhido("$NOME_PADRAO-compartilhado.csv", texto.encodeToByteArray())
        else -> null
    }
}
