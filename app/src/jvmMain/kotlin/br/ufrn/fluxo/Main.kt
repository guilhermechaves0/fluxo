package br.ufrn.fluxo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import br.ufrn.fluxo.presentation.FluxoApp

/** Janela do app no desktop, usada no desenvolvimento com hot reload. */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Fluxo") {
        FluxoApp()
    }
}
