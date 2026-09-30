package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.importador.ImportadorHttp
import br.ufrn.fluxo.adaptadores.memoria.TransacoesEmMemoria
import br.ufrn.fluxo.adaptadores.persistencia.ContasPostgres
import br.ufrn.fluxo.aplicacao.CadastroDeContas
import br.ufrn.fluxo.aplicacao.FonteDeTransacoes
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.ListarTransacoes
import br.ufrn.fluxo.aplicacao.PreverImportacao
import br.ufrn.fluxo.aplicacao.RepositorioDeContas
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.dsl.module

private const val TEMPO_MAXIMO_MS = 30_000L

// Dependências da api, em módulos separados para os testes trocarem uma parte sem mexer nas outras:
// os testes de rota usam `casosDeUso` com repositórios em memória no lugar de `persistencia`.
//
// O Koin só resolve as dependências em tempo de execução. O ModulosTest chama verify() para que uma
// dependência faltando apareça no CI.

/** Cliente HTTP do importador em Go. */
fun importador(urlImportador: String) = module {
    single { clienteDoImportador() }
    single<ImportadorDeExtratos> { ImportadorHttp(url = urlImportador, cliente = get()) }
}

/** Repositórios sobre o PostgreSQL. As transações ainda vêm de uma lista fixa em memória. */
fun persistencia(banco: Database) = module {
    single<RepositorioDeContas> { ContasPostgres(banco) }
    single<FonteDeTransacoes> { TransacoesEmMemoria() }
}

val casosDeUso = module {
    single { CadastroDeContas(contas = get()) }
    single { ListarTransacoes(fonte = get()) }
    single { PreverImportacao(importador = get()) }
}

/** O conjunto usado em produção. */
fun modulosDaAplicacao(urlImportador: String, banco: Database): List<Module> =
    listOf(importador(urlImportador), persistencia(banco), casosDeUso)

private fun clienteDoImportador() = HttpClient(CIO) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(HttpTimeout) { requestTimeoutMillis = TEMPO_MAXIMO_MS }
}
