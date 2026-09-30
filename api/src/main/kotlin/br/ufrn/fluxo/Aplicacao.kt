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

private const val PORTA_PADRAO = 8080

fun main() {
    val porta = System.getenv("PORT")?.toIntOrNull() ?: PORTA_PADRAO
    val urlImportador = System.getenv("FLUXO_IMPORTADOR_URL") ?: "http://localhost:9090"

    // A engine CIO usa corrotinas e ocupa menos memória que a Netty.
    embeddedServer(CIO, port = porta, host = "0.0.0.0") {
        modulo(urlImportador, ConfigBanco.doAmbiente())
    }.start(wait = true)
}

/** Produção: abre o pool de conexões, aplica as migrações e liga os casos de uso ao PostgreSQL. */
fun Application.modulo(urlImportador: String, configBanco: ConfigBanco) {
    val dataSource = criarDataSource(configBanco)
    migrar(dataSource)
    monitor.subscribe(ApplicationStopped) { dataSource.close() }
    configurar(modulosDaAplicacao(urlImportador, Database.connect(dataSource)))
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
