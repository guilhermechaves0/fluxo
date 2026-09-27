package br.ufrn.fluxo.dominio

import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
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

private const val CASAS_DE_CENTAVOS = 2
private const val MAXIMO_DE_DIGITOS_EM_REAIS = 12
private const val PARTES_DA_DATA = 3
private const val DIGITOS_DO_ANO = 4

/**
 * Lê um valor digitado em reais e devolve os centavos, ou `null` se o texto não for um valor.
 *
 * Aceita "12", "12,5", "1.234,56" e "R$ 45,90". O ponto com uma ou duas casas no fim também vale
 * como vírgula ("45.90"), porque é comum digitar assim no teclado numérico.
 */
fun lerReais(texto: String): Long? {
    val limpo = texto.trim().removePrefix("R$").trim()
    val separador = limpo.lastIndexOfAny(charArrayOf(',', '.'))
    val casas = limpo.length - separador - 1
    val temCentavos = separador >= 0 && (limpo[separador] == ',' || casas in 1..CASAS_DE_CENTAVOS)
    val reais = (if (temCentavos) limpo.substring(0, separador) else limpo).replace(".", "")
    val centavos = if (temCentavos) limpo.substring(separador + 1) else ""

    val valido =
        reais.all(Char::isDigit) &&
            centavos.all(Char::isDigit) &&
            centavos.length <= CASAS_DE_CENTAVOS &&
            reais.length <= MAXIMO_DE_DIGITOS_EM_REAIS &&
            (reais + centavos).isNotEmpty()
    if (!valido) return null
    return (reais.ifEmpty { "0" }.toLong() * CENTAVOS_POR_REAL) +
        centavos.padEnd(CASAS_DE_CENTAVOS, '0').toLong()
}

/** Formata a data como no Brasil: 2026-09-05 vira "05/09/2026". */
fun formatarData(data: LocalDate): String =
    "${data.day.toString().padStart(2, '0')}/${data.month.number.toString().padStart(2, '0')}/${data.year}"

/** Lê uma data no formato dd/mm/aaaa. Devolve `null` para texto fora do formato ou data que não existe. */
fun lerData(texto: String): LocalDate? {
    val partes = texto.trim().split("/")
    val numeros = partes.mapNotNull { it.toIntOrNull() }
    if (numeros.size != PARTES_DA_DATA || partes.size != PARTES_DA_DATA || partes.last().length != DIGITOS_DO_ANO) {
        return null
    }
    val (dia, mes, ano) = numeros
    return runCatching { LocalDate(ano, mes, dia) }.getOrNull()
}

private const val MESES_NO_ANO = 12
private val NOMES_DOS_MESES =
    listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )

/** Mês de um ano, usado para mostrar as transações e o saldo de um mês por vez. [numero] vai de 1 a 12. */
data class Mes(val ano: Int, val numero: Int) : Comparable<Mes> {
    init {
        require(numero in 1..MESES_NO_ANO) { "Mês fora de 1 a 12: $numero" }
    }

    fun anterior(): Mes = if (numero == 1) Mes(ano - 1, MESES_NO_ANO) else Mes(ano, numero - 1)

    fun seguinte(): Mes = if (numero == MESES_NO_ANO) Mes(ano + 1, 1) else Mes(ano, numero + 1)

    override fun compareTo(other: Mes): Int = compareValuesBy(this, other, Mes::ano, Mes::numero)
}

/** Mês em que a data cai. */
val LocalDate.mesDoAno: Mes
    get() = Mes(year, month.number)

/** Só as transações do [mes]. */
fun List<Transacao>.doMes(mes: Mes): List<Transacao> = filter { it.data.mesDoAno == mes }

/** "setembro de 2026". */
fun formatarMes(mes: Mes): String = "${NOMES_DOS_MESES[mes.numero - 1]} de ${mes.ano}"

private val SEM_ACENTO =
    mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'é' to 'e', 'ê' to 'e', 'í' to 'i',
        'ó' to 'o', 'ô' to 'o', 'õ' to 'o', 'ú' to 'u', 'ü' to 'u', 'ç' to 'c',
    )

/**
 * Categoria a partir do nome que o banco usa ("Pagamentos/Créditos"), com um id estável tirado do
 * nome ("pagamentos-creditos"). A api e o app chegam ao mesmo id para o mesmo nome.
 */
fun categoriaPeloNome(nome: String): Categoria {
    val semAcento = nome.trim().lowercase().map { SEM_ACENTO[it] ?: it }.joinToString("")
    val id = semAcento.replace(Regex("[^a-z0-9]+"), "-").trim('-')
    return Categoria(id = id.ifEmpty { "outros" }, nome = nome.trim())
}
