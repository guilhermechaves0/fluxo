package br.ufrn.fluxo.presentation.lancamento

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Campo de texto de uma linha com rótulo e mensagem embaixo.
 *
 * Com [erro], o campo fica marcado como inválido e mostra a mensagem, que o leitor de tela também
 * anuncia. Sem erro, mostra a [dica], se houver.
 */
@Composable
fun CampoDoFormulario(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    erro: String? = null,
    dica: String? = null,
    prefixo: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
) {
    val mensagem = erro ?: dica
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        prefix = prefixo?.let { { Text(it) } },
        supportingText = mensagem?.let { { Text(it) } },
        isError = erro != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = ImeAction.Next),
        modifier = modifier.fillMaxWidth(),
    )
}
