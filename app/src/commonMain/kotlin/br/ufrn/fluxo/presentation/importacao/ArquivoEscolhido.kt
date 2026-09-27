package br.ufrn.fluxo.presentation.importacao

import androidx.compose.runtime.Composable

/** Arquivo que a pessoa escolheu no aparelho, já lido na memória. */
class ArquivoEscolhido(val nome: String, val conteudo: ByteArray)

/**
 * Devolve a função que abre o seletor de arquivo do sistema. [aoEscolher] recebe `null` quando a
 * pessoa cancela. Cada plataforma usa o seletor dela (androidMain e jvmMain).
 */
@Composable
expect fun rememberEscolhaDeArquivo(aoEscolher: (ArquivoEscolhido?) -> Unit): () -> Unit
