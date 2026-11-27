package br.ufrn.fluxo

import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

/** Falha se o Koin não conseguir resolver alguma dependência declarada em Modulos.kt. */
class ModulosTest {
    @Test
    fun grafoDeDependenciasResolve() {
        // Confere o importador e os casos de uso, com as portas de persistência em memória. O grafo com
        // o PostgreSQL sobe de verdade no IntegracaoPostgresTest.
        // O canal gRPC é criado dentro do módulo `importador`, não vem do grafo.
        module { includes(importador("localhost:9090"), persistenciaEmMemoria(), casosDeUso) }.verify()
    }
}
