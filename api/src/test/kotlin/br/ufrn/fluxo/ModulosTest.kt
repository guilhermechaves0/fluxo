package br.ufrn.fluxo

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** Falha se o Koin não conseguir resolver alguma dependência declarada em Modulos.kt. */
class ModulosTest {
    @Test
    fun grafoDeDependenciasResolve() {
        // Confere o importador e os casos de uso, com as portas de persistência em memória. O grafo com
        // o PostgreSQL sobe de verdade no IntegracaoPostgresTest.
        //
        // O HttpClient é montado pela função clienteDoImportador(), não pelo Koin: a engine e a
        // configuração dele não são dependências do grafo.
        module { includes(importador("http://localhost:9090"), persistenciaEmMemoria(), casosDeUso) }.verify(
            extraTypes = listOf(HttpClientEngine::class, HttpClientConfig::class),
        )
    }
}
