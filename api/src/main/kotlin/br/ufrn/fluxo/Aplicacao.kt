package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.persistencia.ConfigBanco
import br.ufrn.fluxo.adaptadores.persistencia.criarDataSource
import br.ufrn.fluxo.adaptadores.persistencia.migrar
import br.ufrn.fluxo.adaptadores.web.rotas
import br.ufrn.fluxo.adaptadores.web.tratarErros
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.koin.core.module.Module
import org.koin.ktor.plugin.Koin
import kotlin.system.exitProcess

private const val PORTA_PADRAO = 8080
private const val CODIGO_DE_USO_INVALIDO = 2
private const val ENDERECO_PADRAO_DO_IMPORTADOR = "localhost:9090"

fun main(args: Array<String>) {
    val comando = try {
        escolherComando(args)
    } catch (erro: IllegalArgumentException) {
        System.err.println(erro.message)
        exitProcess(CODIGO_DE_USO_INVALIDO)
    }
    when (comando) {
        Comando.SERVIR -> servir()
        Comando.MIGRAR -> migrarEEncerrar(ConfigBanco.doAmbiente())
    }
}

/**
 * `api migrar`: aplica as migrações e termina com 0. Se o banco não responder ou uma migração falhar, a
 * exceção sobe, o Flyway e o Hikari registram o motivo e o processo termina com 1.
 */
private fun migrarEEncerrar(configBanco: ConfigBanco) {
    criarDataSource(configBanco).use { migrar(it) }
    exitProcess(0)
}

private fun servir() {
    val porta = System.getenv("PORT")?.toIntOrNull() ?: PORTA_PADRAO
    val enderecoImportador = System.getenv("FLUXO_IMPORTADOR_ENDERECO") ?: ENDERECO_PADRAO_DO_IMPORTADOR

    // A engine CIO usa corrotinas e ocupa menos memória que a Netty.
    embeddedServer(CIO, port = porta, host = "0.0.0.0") {
        modulo(enderecoImportador, ConfigBanco.doAmbiente())
    }.start(wait = true)
}

/** Produção: abre o pool de conexões, aplica as migrações e liga os casos de uso ao PostgreSQL. */
fun Application.modulo(enderecoImportador: String, configBanco: ConfigBanco) {
    val dataSource = criarDataSource(configBanco)
    migrar(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }
    configurar(modulosDaAplicacao(enderecoImportador, Database.connect(dataSource)))
}

/**
 * Instala os plugins do Ktor e registra as rotas. Recebe as dependências prontas: os testes de rota
 * passam repositórios em memória, e os de integração passam o PostgreSQL do Testcontainers.
 */
fun Application.configurar(modulos: List<Module>) {
    install(Koin) { modules(modulos) }

    install(ContentNegotiation) { json(Json { explicitNulls = false }) }

    install(CallLogging)

    // Exceções viram respostas no formato da RFC 9457. Ver adaptadores/web/Erros.kt.
    tratarErros()

    rotas()
}
