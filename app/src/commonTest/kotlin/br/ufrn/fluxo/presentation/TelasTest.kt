package br.ufrn.fluxo.presentation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import br.ufrn.fluxo.presentation.navegacao.DetalheDaTransacao
import br.ufrn.fluxo.presentation.navegacao.FluxoNavegacao
import br.ufrn.fluxo.presentation.theme.FluxoTema
import kotlinx.datetime.LocalDate
import kotlin.test.Test

// Testes de interface: montam o app, agem como o usuário e conferem a árvore de semântica, a
// mesma que o TalkBack usa. Os elementos são achados pelo texto visível ou pela descrição.
// Rodam no alvo desktop, com ./gradlew :app:jvmTest.
@OptIn(ExperimentalTestApi::class)
class TelasTest {
    private lateinit var navegacao: NavHostController

    private fun ComposeUiTest.abrirApp() {
        setContent {
            navegacao = rememberNavController()
            FluxoTema { FluxoNavegacao(navController = navegacao, hoje = { LocalDate(2026, 9, 27) }) }
        }
    }

    private fun ComposeUiTest.abrirFormulario() {
        abrirApp()
        onNodeWithContentDescription("Adicionar transação").performClick()
    }

    @Test
    fun listaMostraOSaldoEOTituloComoCabecalho() = runComposeUiTest {
        abrirApp()
        onNodeWithText("Saldo do mês: R$ 3.417,85").assertIsDisplayed()
        onNode(isHeading()).assertTextEquals("Transações")
    }

    @Test
    fun filtroDeReceitasEscondeAsDespesas() = runComposeUiTest {
        abrirApp()
        onNodeWithText("Receitas").performClick()
        onNodeWithText("Salário").assertIsDisplayed()
        onNodeWithText("Aluguel").assertDoesNotExist()
    }

    @Test
    fun cartaoAbreODetalheEVoltarRetornaParaALista() = runComposeUiTest {
        abrirApp()
        onNodeWithText("Aluguel").performClick()
        onNode(isHeading()).assertTextEquals("Aluguel")
        onNodeWithText("Moradia").assertIsDisplayed()
        onNodeWithText("Voltar").performClick()
        onNodeWithText("Saldo do mês: R$ 3.417,85").assertIsDisplayed()
    }

    @Test
    fun rotaComIdAbreADespesaCertaEIdInexistenteAvisa() = runComposeUiTest {
        abrirApp()
        runOnIdle { navegacao.navigate(DetalheDaTransacao(id = "t-03")) }
        onNode(isHeading()).assertTextEquals("Supermercado")
        runOnIdle { navegacao.navigate(DetalheDaTransacao(id = "nao-existe")) }
        onNode(isHeading()).assertTextEquals("Transação não encontrada")
    }

    @Test
    fun salvarComecaDesabilitadoEValorInvalidoMostraMensagem() = runComposeUiTest {
        abrirFormulario()
        onNodeWithText("Salvar").assertIsNotEnabled()
        onNodeWithText("Descrição").performTextInput("Farmácia")
        onNodeWithText("Valor").performTextInput("abc")
        onNodeWithText("Digite um valor como 45,90.").assertIsDisplayed()
        onNodeWithText("Salvar").assertIsNotEnabled()
    }

    @Test
    fun despesaValidaESalvaEApareceNaLista() = runComposeUiTest {
        abrirFormulario()
        onNodeWithText("Descrição").performTextInput("Farmácia")
        onNodeWithText("Valor").performTextInput("37,90")
        onNodeWithText("Mercado").performScrollTo().performClick()
        onNodeWithText("Salvar").performScrollTo().assertIsEnabled().performClick()
        onNodeWithText("Farmácia").assertIsDisplayed()
        onNodeWithText("Saldo do mês: R$ 3.379,95").assertIsDisplayed()
    }

    @Test
    fun receitaNaoPedeCategoria() = runComposeUiTest {
        abrirFormulario()
        onNodeWithText("Categoria").assertIsDisplayed()
        onNodeWithText("Receita").performClick()
        onNodeWithText("Categoria").assertDoesNotExist()
    }
}
