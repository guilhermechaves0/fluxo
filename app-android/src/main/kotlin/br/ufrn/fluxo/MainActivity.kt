package br.ufrn.fluxo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.ufrn.fluxo.presentation.FluxoApp

/** Activity do Android. Só exibe o FluxoApp, que guarda o estado da tela. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FluxoApp() }
    }
}
