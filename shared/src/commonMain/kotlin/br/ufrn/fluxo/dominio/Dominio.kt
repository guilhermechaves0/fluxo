package br.ufrn.fluxo.dominio

import kotlinx.datetime.LocalDate
import kotlin.math.abs

// Entidades e regras usadas pelo app e pela api. Valores em dinheiro são Long em centavos
// para evitar erro de arredondamento de ponto flutuante.

enum class Tipo { RECEITA, DESPESA }

data class Categoria(val id: String, val nome: String)

data class Conta(val id: String, val nome: String)

/** Receita ou despesa. [valorCentavos] é sempre positivo e o sinal vem de [tipo]. */
data class Transacao(
    val id: String,
    val descricao: String,
    val valorCentavos: Long,
    val data: LocalDate,
    val tipo: Tipo,
    val categoria: Categoria? = null,
) {
    init {
        require(descricao.isNotBlank()) { "A descrição é obrigatória." }
        require(valorCentavos > 0) { "O valor é positivo; o sinal vem do tipo." }
    }

    /** Valor positivo para receita e negativo para despesa. */
    val valorComSinal: Long
        get() = if (tipo == Tipo.RECEITA) valorCentavos else -valorCentavos
}

/** Limite de gasto de uma categoria em um mês. */
data class Orcamento(val categoria: Categoria, val ano: Int, val mes: Int, val limiteCentavos: Long)

/** Soma das receitas menos a soma das despesas. */
fun List<Transacao>.saldo(): Long = sumOf { it.valorComSinal }

/** Filtra pelo tipo; com `null`, devolve a lista inteira. */
fun List<Transacao>.porTipo(tipo: Tipo?): List<Transacao> = if (tipo == null) this else filter { it.tipo == tipo }

/** Soma das despesas por categoria. Despesas sem categoria ficam na chave `null`. */
fun List<Transacao>.despesasPorCategoria(): Map<Categoria?, Long> = porTipo(Tipo.DESPESA)
    .groupBy { it.categoria }
    .mapValues { (_, despesas) -> despesas.sumOf { it.valorCentavos } }

private const val CENTAVOS_POR_REAL = 100L
private const val DIGITOS_POR_MILHAR = 3

/** Formata centavos em reais: 123456 vira "R$ 1.234,56". */
fun formatarReais(centavos: Long): String {
    val sinal = if (centavos < 0) "-" else ""
    val absoluto = abs(centavos)
    val reais =
        (absoluto / CENTAVOS_POR_REAL)
            .toString()
            .reversed()
            .chunked(DIGITOS_POR_MILHAR)
            .joinToString(".")
            .reversed()
    val resto = (absoluto % CENTAVOS_POR_REAL).toString().padStart(2, '0')
    return "${sinal}R\$ $reais,$resto"
}
