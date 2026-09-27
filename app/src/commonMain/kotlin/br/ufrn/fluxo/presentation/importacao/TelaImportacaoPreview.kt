package br.ufrn.fluxo.presentation.importacao

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.presentation.theme.FluxoTema

// Previews dos passos da importação: início, prévia e falha.

@Preview
@Composable
fun TelaImportacaoInicialPreview() {
    FluxoTema {
        TelaImportacao(EstadoDaImportacao.Inicial, aoEscolherArquivo = {}, aoConfirmar = {}, aoVoltar = {})
    }
}

@Preview
@Composable
fun TelaImportacaoPreviaPreview() {
    FluxoTema(escuro = true) {
        TelaImportacao(
            EstadoDaImportacao.Previa(
                nomeDoArquivo = "extrato-setembro.ofx",
                origem = "Nubank",
                novas = transacoesDeExemplo.take(3),
                repetidas = 2,
                ignoradas = 1,
            ),
            aoEscolherArquivo = {},
            aoConfirmar = {},
            aoVoltar = {},
        )
    }
}

@Preview
@Composable
fun TelaImportacaoFalhaPreview() {
    FluxoTema {
        TelaImportacao(
            EstadoDaImportacao.Falha("Não consegui falar com a api em http://localhost:8081. Ela está rodando?"),
            aoEscolherArquivo = {},
            aoConfirmar = {},
            aoVoltar = {},
        )
    }
}
