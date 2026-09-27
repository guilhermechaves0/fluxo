package br.ufrn.fluxo.presentation.transacoes

import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.formatarMes
import br.ufrn.fluxo.dominio.mesDoAno
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlin.math.roundToInt

/** Id usado no filtro para os gastos que o banco não classificou. */
const val ID_SEM_CATEGORIA = "sem-categoria"

private const val CEM = 100

/** Quanto uma categoria pesa no gasto do mês. [fracao] vai de 0 a 1. */
data class FatiaDoMes(val categoria: Categoria?, val centavos: Long, val fracao: Float) {
    val id: String get() = categoria?.id ?: ID_SEM_CATEGORIA
    val nome: String get() = categoria?.nome ?: "Sem categoria"

    /** "80%", ou "<1%" para as fatias pequenas demais para arredondar. */
    val percentual: String
        get() = (fracao * CEM).roundToInt().let { if (it == 0) "<1%" else "$it%" }
}

/** Gastos agrupados por categoria, do maior para o menor. Entradas não entram na conta. */
fun List<Transacao>.fatiasDeGasto(): List<FatiaDoMes> {
    val gastos = filter { it.tipo == Tipo.DESPESA }
    val total = gastos.sumOf { it.valorCentavos }
    if (total == 0L) return emptyList()
    return gastos
        .groupBy { it.categoria }
        .map { (categoria, lista) ->
            val soma = lista.sumOf { it.valorCentavos }
            FatiaDoMes(categoria, soma, soma.toFloat() / total)
        }.sortedByDescending { it.centavos }
}

/**
 * Só as transações da categoria escolhida; com `null`, todas. A fatia "Sem categoria" vem da barra de
 * gastos, então mostra só os gastos sem categoria, e não as entradas.
 */
fun List<Transacao>.daCategoria(id: String?): List<Transacao> = when (id) {
    null -> this
    ID_SEM_CATEGORIA -> filter { it.categoria == null && it.tipo == Tipo.DESPESA }
    else -> filter { it.categoria?.id == id }
}

/** Dias com transações, do mais recente para o mais antigo, cada um com as suas transações. */
fun List<Transacao>.porDia(): List<Pair<LocalDate, List<Transacao>>> =
    groupBy { it.data }.toList().sortedByDescending { it.first }

/** "Hoje", "Ontem", "10 de setembro" ou, em outro ano, "30 de dezembro de 2025". */
fun rotuloDoDia(dia: LocalDate, hoje: LocalDate): String = when (dia) {
    hoje -> "Hoje"

    hoje.minus(DatePeriod(days = 1)) -> "Ontem"

    else -> {
        val mes = formatarMes(dia.mesDoAno)
        val nomeDoMes = mes.substringBefore(" de ")
        if (dia.year == hoje.year) "${dia.day} de $nomeDoMes" else "${dia.day} de $mes"
    }
}
