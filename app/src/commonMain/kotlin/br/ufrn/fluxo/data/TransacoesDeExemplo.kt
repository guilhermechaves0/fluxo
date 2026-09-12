package br.ufrn.fluxo.data

import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Tipo.DESPESA
import br.ufrn.fluxo.dominio.Tipo.RECEITA
import br.ufrn.fluxo.dominio.Transacao
import kotlinx.datetime.LocalDate

// Dados fixos exibidos pela tela até a integração com a api, prevista para a Sprint 3.

private val moradia = Categoria(id = "moradia", nome = "Moradia")
private val mercado = Categoria(id = "mercado", nome = "Mercado")
private val alimentacao = Categoria(id = "alimentacao", nome = "Alimentação")
private val transporte = Categoria(id = "transporte", nome = "Transporte")

private fun transacao(
    id: String,
    descricao: String,
    centavos: Long,
    data: String,
    tipo: Tipo,
    categoria: Categoria? = null,
) = Transacao(id, descricao, centavos, LocalDate.parse(data), tipo, categoria)

val transacoesDeExemplo: List<Transacao> =
    listOf(
        transacao("t-01", "Salário", centavos = 450_000, data = "2026-09-05", RECEITA),
        transacao("t-02", "Aluguel", centavos = 150_000, data = "2026-09-05", DESPESA, moradia),
        transacao("t-03", "Supermercado", centavos = 31_245, data = "2026-09-06", DESPESA, mercado),
        transacao("t-04", "Freelance", centavos = 80_000, data = "2026-09-08", RECEITA),
        transacao("t-05", "Corrida de aplicativo", centavos = 2_380, data = "2026-09-09", DESPESA, transporte),
        transacao("t-06", "Padaria", centavos = 4_590, data = "2026-09-10", DESPESA, alimentacao),
    )

/** Despesa criada pelo botão +. O formulário de lançamento substitui este atalho na Sprint 1. */
fun novaTransacaoDeExemplo(sequencia: Int): Transacao =
    transacao("nova-$sequencia", "Café", centavos = 850, data = "2026-09-12", DESPESA, alimentacao)
