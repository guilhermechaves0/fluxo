package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.memoria.TransacoesEmMemoria
import br.ufrn.fluxo.aplicacao.FonteDeTransacoes
import br.ufrn.fluxo.aplicacao.ListarTransacoes
import org.koin.dsl.module

/**
 * Dependências da api.
 *
 * O Koin só resolve as dependências em tempo de execução. O ModulosTest chama verify() para que
 * uma dependência faltando apareça no CI.
 */
fun modulosDaAplicacao() = module {
    single<FonteDeTransacoes> { TransacoesEmMemoria() }
    single { ListarTransacoes(fonte = get()) }
}
