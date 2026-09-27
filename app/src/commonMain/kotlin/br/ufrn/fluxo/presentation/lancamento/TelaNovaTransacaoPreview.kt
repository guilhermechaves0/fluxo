package br.ufrn.fluxo.presentation.lancamento

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.ufrn.fluxo.data.categoriasDeExemplo
import br.ufrn.fluxo.presentation.theme.FluxoTema

// Previews do formulário vazio, com erro e pronto para salvar.

@Preview
@Composable
fun TelaNovaTransacaoVaziaPreview() {
    FluxoTema {
        TelaNovaTransacao(
            rascunho = RascunhoTransacao(data = "27/09/2026"),
            categorias = categoriasDeExemplo,
            aoMudar = {},
            aoSalvar = {},
            aoVoltar = {},
        )
    }
}

@Preview
@Composable
fun TelaNovaTransacaoComErroPreview() {
    FluxoTema(escuro = true) {
        TelaNovaTransacao(
            rascunho = RascunhoTransacao(descricao = "Farmácia", valor = "abc", data = "31/02/2026"),
            categorias = categoriasDeExemplo,
            aoMudar = {},
            aoSalvar = {},
            aoVoltar = {},
        )
    }
}

@Preview
@Composable
fun TelaNovaTransacaoValidaPreview() {
    FluxoTema {
        TelaNovaTransacao(
            rascunho =
            RascunhoTransacao(
                descricao = "Farmácia",
                valor = "37,90",
                data = "27/09/2026",
                categoria = categoriasDeExemplo.first(),
            ),
            categorias = categoriasDeExemplo,
            aoMudar = {},
            aoSalvar = {},
            aoVoltar = {},
        )
    }
}
