package br.ufrn.fluxo.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Tema do Fluxo, claro e escuro, seguindo o sistema. Base fria e calma com o âmbar como único acento
 * (dinheiro que entra) e uma cor por categoria, que o app inteiro usa do mesmo jeito.
 */
@Composable
fun FluxoTema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoresDoFluxo provides if (escuro) coresEscuras else coresClaras) {
        MaterialTheme(
            colorScheme = if (escuro) esquemaEscuro else esquemaClaro,
            typography = tipografiaDoFluxo(),
            content = conteudo,
        )
    }
}
