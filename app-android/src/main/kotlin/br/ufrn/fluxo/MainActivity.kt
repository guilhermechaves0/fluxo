package br.ufrn.fluxo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.ufrn.fluxo.presentation.FluxoApp
import br.ufrn.fluxo.presentation.importacao.arquivoCompartilhado

/**
 * Activity do Android. Exibe o FluxoApp e, quando é aberta pelo "Compartilhar" de outro app (o
 * extrato em CSV do app do banco, por exemplo), entrega o arquivo para a tela de importação.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Depois de girar a tela o arquivo já foi entregue; ler de novo repetiria a importação.
        val compartilhado = if (savedInstanceState == null) arquivoCompartilhado(this, intent) else null
        setContent { FluxoApp(arquivoCompartilhado = compartilhado) }
    }
}
