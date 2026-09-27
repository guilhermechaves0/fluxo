package br.ufrn.fluxo.presentation.navegacao

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import br.ufrn.fluxo.data.ApiDoFluxo
import br.ufrn.fluxo.data.FalhaNaApi
import br.ufrn.fluxo.data.categoriasDeExemplo
import br.ufrn.fluxo.data.transacoesDeExemplo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarData
import br.ufrn.fluxo.presentation.detalhe.TelaDetalheTransacao
import br.ufrn.fluxo.presentation.importacao.ArquivoEscolhido
import br.ufrn.fluxo.presentation.importacao.EstadoDaImportacao
import br.ufrn.fluxo.presentation.importacao.TelaImportacao
import br.ufrn.fluxo.presentation.importacao.montarPrevia
import br.ufrn.fluxo.presentation.importacao.rememberEscolhaDeArquivo
import br.ufrn.fluxo.presentation.lancamento.RascunhoTransacao
import br.ufrn.fluxo.presentation.lancamento.TelaNovaTransacao
import br.ufrn.fluxo.presentation.lancamento.paraTransacao
import br.ufrn.fluxo.presentation.transacoes.FiltroTransacoes
import br.ufrn.fluxo.presentation.transacoes.TelaTransacoes
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Grafo de navegação do app.
 *
 * A lista de transações e o filtro ficam aqui, acima do NavHost, para que as três telas vejam o
 * mesmo estado. As telas recebem lambdas e não conhecem o NavController. Na Sprint 2 este estado
 * vai para um ViewModel. Os testes passam [transacoesIniciais], [hoje] e [api] para ter dados fixos.
 */
@Composable
fun FluxoNavegacao(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    transacoesIniciais: List<Transacao> = transacoesDeExemplo,
    hoje: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
    api: ApiDoFluxo = remember { ApiDoFluxo() },
    arquivoCompartilhado: ArquivoEscolhido? = null,
) {
    var transacoes by remember { mutableStateOf(transacoesIniciais) }
    var filtro by remember { mutableStateOf(FiltroTransacoes.TODAS) }
    var proximoId by remember { mutableStateOf(1) }
    var importacao by remember { mutableStateOf<EstadoDaImportacao>(EstadoDaImportacao.Inicial) }
    val escopo = rememberCoroutineScope()
    val importar: (ArquivoEscolhido) -> Unit = { arquivo ->
        importacao = EstadoDaImportacao.Lendo(arquivo.nome)
        escopo.launch {
            importacao =
                try {
                    montarPrevia(arquivo.nome, api.previaDeImportacao(arquivo.nome, arquivo.conteudo), transacoes)
                } catch (e: FalhaNaApi) {
                    EstadoDaImportacao.Falha(e.message ?: "Não consegui ler o extrato.")
                }
        }
    }
    val escolherArquivo = rememberEscolhaDeArquivo { arquivo -> arquivo?.let(importar) }

    // Extrato recebido pelo "Compartilhar" de outro app: abre direto na importação.
    LaunchedEffect(arquivoCompartilhado) {
        arquivoCompartilhado?.let {
            navController.navigate(ImportarExtrato)
            importar(it)
        }
    }

    NavHost(navController, startDestination = ListaDeTransacoes, modifier = modifier) {
        composable<ListaDeTransacoes> {
            TelaTransacoes(
                transacoes = transacoes,
                filtro = filtro,
                aoTrocarFiltro = { filtro = it },
                aoAbrir = { id -> navController.navigate(DetalheDaTransacao(id)) },
                aoAdicionar = { navController.navigate(NovaTransacao) },
                aoImportar = {
                    importacao = EstadoDaImportacao.Inicial
                    navController.navigate(ImportarExtrato)
                },
            )
        }
        composable<ImportarExtrato> {
            TelaImportacao(
                estado = importacao,
                aoEscolherArquivo = escolherArquivo,
                aoConfirmar = {
                    (importacao as? EstadoDaImportacao.Previa)?.let { previa ->
                        transacoes = transacoes + previa.novas
                        importacao = EstadoDaImportacao.Inicial
                        navController.popBackStack()
                    }
                },
                aoVoltar = { navController.popBackStack() },
            )
        }
        composable<DetalheDaTransacao>(
            deepLinks = listOf(navDeepLink<DetalheDaTransacao>(basePath = ENDERECO_DO_DETALHE)),
        ) { entrada ->
            val rota = entrada.toRoute<DetalheDaTransacao>()
            TelaDetalheTransacao(
                transacao = transacoes.find { it.id == rota.id },
                aoVoltar = { navController.popBackStack() },
            )
        }
        composable<NovaTransacao> {
            var rascunho by remember { mutableStateOf(RascunhoTransacao(data = formatarData(hoje()))) }
            TelaNovaTransacao(
                rascunho = rascunho,
                categorias = categoriasDeExemplo,
                aoMudar = { rascunho = it },
                aoSalvar = {
                    rascunho.paraTransacao(id = "manual-$proximoId")?.let { nova ->
                        transacoes = transacoes + nova
                        proximoId++
                        navController.popBackStack()
                    }
                },
                aoVoltar = { navController.popBackStack() },
            )
        }
    }
}
