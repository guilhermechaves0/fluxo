package br.ufrn.fluxo.presentation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import br.ufrn.fluxo.data.ApiDoFluxo
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.presentation.importacao.ArquivoEscolhido
import br.ufrn.fluxo.presentation.importacao.EstadoDaImportacao
import br.ufrn.fluxo.presentation.importacao.TelaImportacao
import br.ufrn.fluxo.presentation.navegacao.DetalheDaTransacao
import br.ufrn.fluxo.presentation.navegacao.FluxoNavegacao
import br.ufrn.fluxo.presentation.theme.FluxoTema
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

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

    @Test
    fun importarExtratoAbreATelaEVoltaParaALista() = runComposeUiTest {
        abrirApp()
        onNodeWithText("Importar extrato").performClick()
        onNode(isHeading()).assertTextEquals("Importar extrato")
        onNodeWithText("Escolher arquivo").assertIsEnabled()
        onNodeWithText("Voltar").performClick()
        onNodeWithText("Saldo do mês: R$ 3.417,85").assertIsDisplayed()
    }

    @Test
    fun previaMostraOQueEhNovoEConfirma() = runComposeUiTest {
        var confirmou = false
        val previa =
            EstadoDaImportacao.Previa(
                nomeDoArquivo = "extrato.ofx",
                origem = "Nubank",
                novas = transacoesDeExemplo.take(2),
                repetidas = 1,
                ignoradas = 0,
            )
        setContent {
            FluxoTema {
                TelaImportacao(previa, aoEscolherArquivo = {}, aoConfirmar = { confirmou = true }, aoVoltar = {})
            }
        }
        onNodeWithText("Nubank · extrato.ofx", substring = true).assertIsDisplayed()
        onNodeWithText("2 lançamentos novos, 1 já estava na lista", substring = true).assertIsDisplayed()
        onNodeWithText("Salário").assertIsDisplayed()
        onNodeWithText("Importar 2 lançamentos").performClick()
        assertTrue(confirmou)
    }

    @Test
    fun falhaNaLeituraMostraAMensagemEDeixaTentarDeNovo() = runComposeUiTest {
        setContent {
            FluxoTema {
                TelaImportacao(
                    EstadoDaImportacao.Falha("o cabeçalho \"a, b\" não tem colunas que eu reconheça"),
                    aoEscolherArquivo = {},
                    aoConfirmar = {},
                    aoVoltar = {},
                )
            }
        }
        onNodeWithText("não tem colunas que eu reconheça", substring = true).assertIsDisplayed()
        onNodeWithText("Escolher outro arquivo").assertIsEnabled()
    }

    @Test
    fun extratoCompartilhadoPorOutroAppAbreNaImportacaoEEntraNaLista() = runComposeUiTest {
        val previa =
            """{"formato":"CSV","origem":"Banco do Brasil","transacoes":[{"id":"imp-bancodobrasil-1",""" +
                """"descricao":"CONTA DE LUZ","valorCentavos":18990,"data":"2026-09-20","tipo":"DESPESA"}],"ignorados":[]}"""
        val api =
            ApiDoFluxo(
                url = "http://api",
                cliente =
                HttpClient(
                    MockEngine {
                        respond(previa, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                    },
                ),
            )
        setContent {
            navegacao = rememberNavController()
            FluxoTema {
                FluxoNavegacao(
                    navController = navegacao,
                    hoje = { LocalDate(2026, 9, 27) },
                    api = api,
                    arquivoCompartilhado = ArquivoEscolhido("extrato-bb.csv", "Data;Valor".encodeToByteArray()),
                )
            }
        }
        waitUntil(timeoutMillis = TEMPO_DA_API_MS) {
            onAllNodesWithText("Importar 1 lançamento").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Banco do Brasil · extrato-bb.csv", substring = true).assertIsDisplayed()
        onNodeWithText("Importar 1 lançamento").performClick()
        onNodeWithText("CONTA DE LUZ").assertIsDisplayed()
        onNodeWithText("Saldo do mês: R$ 3.227,95").assertIsDisplayed()
    }

    private companion object {
        const val TEMPO_DA_API_MS = 5_000L
    }
}
