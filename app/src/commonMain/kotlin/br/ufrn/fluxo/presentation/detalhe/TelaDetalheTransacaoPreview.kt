package br.ufrn.fluxo.presentation.detalhe

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.presentation.theme.FluxoTema

// Previews do detalhe de uma despesa e do aviso de transação inexistente.

@Preview
@Composable
fun TelaDetalheTransacaoPreview() {
    FluxoTema {
        TelaDetalheTransacao(transacao = transacoesDeExemplo[1], aoVoltar = {})
    }
}

@Preview
@Composable
fun TelaDetalheTransacaoNaoEncontradaPreview() {
    FluxoTema(escuro = true) {
        TelaDetalheTransacao(transacao = null, aoVoltar = {})
    }
}
