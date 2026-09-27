package br.ufrn.fluxo.presentation.lancamento

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo

/**
 * Formulário de lançamento manual.
 *
 * A tela não guarda estado: recebe o rascunho e devolve cada alteração por [aoMudar]. O botão
 * Salvar só fica habilitado quando o rascunho vira uma transação válida.
 */
@Composable
fun TelaNovaTransacao(
    rascunho: RascunhoTransacao,
    categorias: List<Categoria>,
    aoMudar: (RascunhoTransacao) -> Unit,
    aoSalvar: () -> Unit,
    aoVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val erros = rascunho.erros()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = aoVoltar) { Text("Voltar") }
        Text(
            "Nova transação",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        SeletorDeTipo(selecionado = rascunho.tipo, aoSelecionar = { aoMudar(rascunho.comTipo(it)) })
        CampoDoFormulario(
            valor = rascunho.descricao,
            aoMudar = { aoMudar(rascunho.copy(descricao = it)) },
            rotulo = "Descrição",
            erro = erros.descricao,
            dica = "Onde ou com o quê, como Padaria",
        )
        CampoDoFormulario(
            valor = rascunho.valor,
            aoMudar = { aoMudar(rascunho.copy(valor = it)) },
            rotulo = "Valor",
            erro = erros.valor,
            prefixo = "R$ ",
            teclado = KeyboardType.Decimal,
        )
        CampoDoFormulario(
            valor = rascunho.data,
            aoMudar = { aoMudar(rascunho.copy(data = it)) },
            rotulo = "Data",
            erro = erros.data,
            dica = "dd/mm/aaaa",
        )
        if (rascunho.tipo == Tipo.DESPESA) {
            SeletorDeCategoria(
                categorias = categorias,
                selecionada = rascunho.categoria,
                aoSelecionar = { aoMudar(rascunho.copy(categoria = it)) },
            )
        }
        Button(onClick = aoSalvar, enabled = rascunho.podeSalvar, modifier = Modifier.fillMaxWidth()) {
            Text("Salvar")
        }
    }
}
