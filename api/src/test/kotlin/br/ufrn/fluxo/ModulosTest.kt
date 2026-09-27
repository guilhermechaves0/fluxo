package br.ufrn.fluxo

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.koin.test.verify.verify
import kotlin.test.Test

/** Falha se o Koin não conseguir resolver alguma dependência declarada em Modulos.kt. */
class ModulosTest {
    @Test
    fun grafoDeDependenciasResolve() {
        // O HttpClient é montado pela função clienteDoImportador(), não pelo Koin: a engine e a
        // configuração dele não são dependências do grafo.
        modulosDaAplicacao("http://localhost:9090").verify(
            extraTypes = listOf(HttpClientEngine::class, HttpClientConfig::class),
        )
    }
}
