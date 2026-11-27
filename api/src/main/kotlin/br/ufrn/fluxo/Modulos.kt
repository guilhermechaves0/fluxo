package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.importador.ImportadorGrpc
import br.ufrn.fluxo.adaptadores.persistencia.ContasPostgres
import br.ufrn.fluxo.adaptadores.persistencia.TransacoesPostgres
import br.ufrn.fluxo.aplicacao.CadastroDeContas
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.PreverImportacao
import br.ufrn.fluxo.aplicacao.RepositorioDeContas
import br.ufrn.fluxo.aplicacao.RepositorioDeTransacoes
import br.ufrn.fluxo.aplicacao.TransacoesDaConta
import io.grpc.ManagedChannelBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.dsl.onClose

// Dependências da api, em módulos separados para os testes trocarem uma parte sem mexer nas outras:
// os testes de rota usam `casosDeUso` com repositórios em memória no lugar de `persistencia`.
//
// O Koin só resolve as dependências em tempo de execução. O ModulosTest chama verify() para que uma
// dependência faltando apareça no CI.

/**
 * Cliente gRPC do importador em Go. [enderecoImportador] é `host:porta`. O canal pertence ao adaptador, e o
 * Koin o fecha quando a aplicação para.
 */
fun importador(enderecoImportador: String) = module {
    single<ImportadorDeExtratos> {
        // Sem TLS: o importador só é alcançável pela rede interna do compose ou da plataforma.
        ImportadorGrpc(ManagedChannelBuilder.forTarget(enderecoImportador).usePlaintext().build())
    } onClose { (it as? AutoCloseable)?.close() }
}

/** Repositórios sobre o PostgreSQL. */
fun persistencia(banco: Database) = module {
    single<RepositorioDeContas> { ContasPostgres(banco) }
    single<RepositorioDeTransacoes> { TransacoesPostgres(banco) }
}

val casosDeUso = module {
    single { CadastroDeContas(contas = get()) }
    single { TransacoesDaConta(contas = get(), transacoes = get()) }
    single { PreverImportacao(importador = get()) }
}

/** O conjunto usado em produção. */
fun modulosDaAplicacao(enderecoImportador: String, banco: Database): List<Module> =
    listOf(importador(enderecoImportador), persistencia(banco), casosDeUso)
