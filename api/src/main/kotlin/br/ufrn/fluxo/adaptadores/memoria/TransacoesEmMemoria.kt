package br.ufrn.fluxo.adaptadores.memoria

import br.ufrn.fluxo.aplicacao.FonteDeTransacoes
import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate

/** Transações fixas em memória. Na Sprint 1 este adaptador dá lugar ao Postgres com Flyway. */
class TransacoesEmMemoria : FonteDeTransacoes {
    private val mercado = Categoria(id = "mercado", nome = "Mercado")

    private val transacoes =
        listOf(
            Transacao("t-01", "Salário", 450_000, LocalDate.parse("2026-09-05"), Tipo.RECEITA),
            Transacao("t-02", "Supermercado", 31_245, LocalDate.parse("2026-09-06"), Tipo.DESPESA, mercado),
            Transacao("t-03", "Padaria", 4_590, LocalDate.parse("2026-09-10"), Tipo.DESPESA),
        )

    override suspend fun listar(): List<Transacao> = transacoes
}
