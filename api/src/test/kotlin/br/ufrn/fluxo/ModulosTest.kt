package br.ufrn.fluxo

import org.koin.test.verify.verify
import kotlin.test.Test

/** Falha se o Koin não conseguir resolver alguma dependência declarada em Modulos.kt. */
class ModulosTest {
    @Test
    fun grafoDeDependenciasResolve() {
        modulosDaAplicacao().verify()
    }
}
