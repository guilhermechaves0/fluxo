package br.ufrn.fluxo.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Base fria para passar calma: névoa e papel no claro, ardósia no escuro. O âmbar é o único acento
// e marca o dinheiro que entra. Gasto fica na cor do texto, sem vermelho.
private val Nevoa = Color(0xFFF2F4F7)
private val Papel = Color(0xFFFFFFFF)
private val Tinta = Color(0xFF1D2633)
private val Grafite = Color(0xFF5E6A7A)
private val Fio = Color(0xFFDDE2E8)
private val AmbarEscuro = Color(0xFF8F5400)
private val Ambar = Color(0xFFE39A2D)

private val Ardosia = Color(0xFF141C27)
private val PapelNoturno = Color(0xFF1C2533)
private val TintaClara = Color(0xFFE7EBF0)
private val GrafiteClaro = Color(0xFF97A3B3)
private val FioNoturno = Color(0xFF2A3442)
private val AmbarClaro = Color(0xFFF0B252)

internal val esquemaClaro =
    lightColorScheme(
        primary = AmbarEscuro,
        onPrimary = Papel,
        primaryContainer = Ambar,
        onPrimaryContainer = Color(0xFF3F2400),
        secondary = Grafite,
        onSecondary = Papel,
        secondaryContainer = Color(0xFFE3E8EE),
        onSecondaryContainer = Tinta,
        background = Nevoa,
        onBackground = Tinta,
        surface = Papel,
        onSurface = Tinta,
        surfaceVariant = Color(0xFFE6EAEF),
        onSurfaceVariant = Grafite,
        surfaceContainerLowest = Papel,
        surfaceContainerLow = Color(0xFFF7F8FA),
        surfaceContainer = Nevoa,
        surfaceContainerHigh = Color(0xFFEBEEF2),
        surfaceContainerHighest = Color(0xFFE3E7EC),
        outline = Color(0xFFB3BCC7),
        outlineVariant = Fio,
    )

internal val esquemaEscuro =
    darkColorScheme(
        primary = AmbarClaro,
        onPrimary = Color(0xFF3A2300),
        primaryContainer = AmbarClaro,
        onPrimaryContainer = Color(0xFF3A2300),
        secondary = GrafiteClaro,
        onSecondary = Ardosia,
        secondaryContainer = Color(0xFF2A3544),
        onSecondaryContainer = TintaClara,
        background = Ardosia,
        onBackground = TintaClara,
        surface = PapelNoturno,
        onSurface = TintaClara,
        surfaceVariant = Color(0xFF232E3C),
        onSurfaceVariant = GrafiteClaro,
        surfaceContainerLowest = Color(0xFF101720),
        surfaceContainerLow = Color(0xFF18212D),
        surfaceContainer = PapelNoturno,
        surfaceContainerHigh = Color(0xFF232E3C),
        surfaceContainerHighest = Color(0xFF2A3544),
        outline = Color(0xFF4A5666),
        outlineVariant = FioNoturno,
    )

/**
 * Cores que o Material 3 não tem papel para: as de categoria, que são o elemento marcante do app, e a
 * da categoria vazia. Os tons são apagados e ficam longe do âmbar, para não competir com a entrada.
 */
@Immutable
data class CoresDoFluxo(val categorias: List<Color>, val semCategoria: Color)

internal val coresClaras =
    CoresDoFluxo(
        categorias =
        listOf(
            Color(0xFF5E81AC), // azul
            Color(0xFF6F9A7C), // sálvia
            Color(0xFFB06A82), // ameixa
            Color(0xFF7F74B8), // lavanda
            Color(0xFF3F9A97), // verde-água
            Color(0xFF4C7F99), // petróleo
            Color(0xFF9A6BA8), // orquídea
            Color(0xFF7E8C4E), // musgo
            Color(0xFF5A9BC4), // céu
            Color(0xFFA07C86), // rosa-ardósia
        ),
        semCategoria = Color(0xFFC9CFD7),
    )

internal val coresEscuras =
    CoresDoFluxo(
        categorias =
        listOf(
            Color(0xFF8FAFD6),
            Color(0xFF9CC4A7),
            Color(0xFFD796AC),
            Color(0xFFABA2DD),
            Color(0xFF74C4C1),
            Color(0xFF86B3CA),
            Color(0xFFC39AD0),
            Color(0xFFB0BD80),
            Color(0xFF93C3E2),
            Color(0xFFCBAAB2),
        ),
        semCategoria = Color(0xFF3A4656),
    )

val LocalCoresDoFluxo = staticCompositionLocalOf { coresClaras }

// As categorias mais comuns têm cor fixa, para o app ficar "mapeado" do mesmo jeito todo mês. As
// outras recebem uma cor pelo id, que também não muda entre importações.
private val corFixa =
    mapOf(
        "moradia" to 0,
        "mercado" to 1,
        "supermercados" to 1,
        "alimentacao" to 2,
        "restaurantes" to 2,
        "transporte" to 3,
        "saude" to 4,
        "servicos" to 5,
        "vestuario" to 6,
        "lazer" to 7,
        "viagens" to 8,
        "compras-parceladas" to 9,
    )

/** Cor da categoria pelo id; sem categoria, o cinza neutro. */
fun CoresDoFluxo.daCategoria(id: String?): Color {
    if (id == null) return semCategoria
    val indice = corFixa[id] ?: id.hashCode().mod(categorias.size)
    return categorias[indice]
}
