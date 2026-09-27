package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.importador.ImportadorHttp
import br.ufrn.fluxo.adaptadores.memoria.TransacoesEmMemoria
import br.ufrn.fluxo.aplicacao.FonteDeTransacoes
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.ListarTransacoes
import br.ufrn.fluxo.aplicacao.PreverImportacao
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

private const val TEMPO_MAXIMO_MS = 30_000L

/**
 * Dependências da api.
 *
 * O Koin só resolve as dependências em tempo de execução. O ModulosTest chama verify() para que
 * uma dependência faltando apareça no CI.
 */
fun modulosDaAplicacao(urlImportador: String) = module {
    single<FonteDeTransacoes> { TransacoesEmMemoria() }
    single { ListarTransacoes(fonte = get()) }
    single { clienteDoImportador() }
    single<ImportadorDeExtratos> { ImportadorHttp(url = urlImportador, cliente = get()) }
    single { PreverImportacao(importador = get()) }
}

private fun clienteDoImportador() = HttpClient(CIO) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = TEMPO_MAXIMO_MS }
}
