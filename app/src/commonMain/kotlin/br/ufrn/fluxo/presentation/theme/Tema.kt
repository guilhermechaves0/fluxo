package br.ufrn.fluxo.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val coresClaras =
    lightColorScheme(
        primary = Color(0xFF0B6E4F),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF3E5C76),
        error = Color(0xFFB3261E),
    )

private val coresEscuras =
    darkColorScheme(
        primary = Color(0xFF6CCF9F),
        onPrimary = Color(0xFF003824),
        secondary = Color(0xFF9DB4CF),
        error = Color(0xFFF2B8B5),
    )

/** Tema Material 3 com esquema claro e escuro. Receitas usam a cor primária e despesas, a cor de erro. */
@Composable
fun FluxoTema(escuro: Boolean = isSystemInDarkTheme(), conteudo: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (escuro) coresEscuras else coresClaras,
        typography = Typography(),
        content = conteudo,
    )
}
