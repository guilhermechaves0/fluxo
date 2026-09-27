package br.ufrn.fluxo.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import br.ufrn.fluxo.recursos.Res
import br.ufrn.fluxo.recursos.figtree_light
import br.ufrn.fluxo.recursos.figtree_medium
import br.ufrn.fluxo.recursos.figtree_regular
import org.jetbrains.compose.resources.Font

/** Figtree: geométrica e amigável, com algarismos de largura fixa para os valores alinharem. */
@Composable
private fun figtree() = FontFamily(
    Font(Res.font.figtree_light, FontWeight.Light),
    Font(Res.font.figtree_regular, FontWeight.Normal),
    Font(Res.font.figtree_medium, FontWeight.Medium),
)

// "tnum" liga os algarismos tabulares: 1.500,00 e 312,45 terminam com a vírgula na mesma coluna.
private fun estilo(familia: FontFamily, tamanho: Int, entrelinha: Int, peso: FontWeight = FontWeight.Normal) =
    TextStyle(
        fontFamily = familia,
        fontWeight = peso,
        fontSize = tamanho.sp,
        lineHeight = entrelinha.sp,
        fontFeatureSettings = "tnum",
    )

/** Escala contida, em torno do corpo de 16: o destaque do app é a cor das categorias, não o tamanho da letra. */
@Composable
internal fun tipografiaDoFluxo(): Typography {
    val f = figtree()
    return Typography(
        displaySmall = estilo(f, 36, 44, FontWeight.Light),
        headlineMedium = estilo(f, 28, 36, FontWeight.Light),
        headlineSmall = estilo(f, 24, 32, FontWeight.Light),
        titleLarge = estilo(f, 21, 28, FontWeight.Medium),
        titleMedium = estilo(f, 17, 24, FontWeight.Medium),
        titleSmall = estilo(f, 14, 20, FontWeight.Medium),
        bodyLarge = estilo(f, 16, 24),
        bodyMedium = estilo(f, 14, 20),
        bodySmall = estilo(f, 13, 18),
        labelLarge = estilo(f, 14, 20, FontWeight.Medium),
        labelMedium = estilo(f, 12, 16, FontWeight.Medium),
        labelSmall = estilo(f, 11, 16, FontWeight.Medium),
    )
}
