package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.RoutingCall
import io.ktor.server.util.getOrFail
import kotlinx.datetime.LocalDate

// Leitura dos parâmetros de caminho e de consulta e dos campos de texto do corpo. Tipo ou formato
// errado (`?pagina=abc`, data que não existe) é requisição malformada e responde 400. O mesmo vale para o
// que o PostgreSQL não grava: texto com o caractere nulo e data de ano fora de 1 a 9999. Valor bem formado
// que quebra uma regra (`?tamanho=500`) responde 422, e quem decide isso é a aplicação.

private val FORMATO_DO_MES = Regex("""(\d{4})-(\d{2})""")
private const val CARACTERE_NULO = '\u0000'
private const val PRIMEIRO_ANO = 1
private const val ULTIMO_ANO = 9999

/**
 * Parâmetro ou campo com tipo ou formato errado. A mensagem já diz o que era esperado e o que veio, e
 * é ela que vai na resposta, e não a da exceção de origem.
 */
class FormatoInvalido(mensagem: String, causa: Throwable? = null) : BadRequestException(mensagem, causa)

/** Parâmetro de consulta inteiro, ou `null` se não veio. */
fun ApplicationCall.inteiro(nome: String): Int? = request.queryParameters[nome]?.let {
    it.toIntOrNull() ?: throw FormatoInvalido("$nome deve ser um número inteiro, e veio \"$it\"")
}

/** Parâmetro de consulta de texto, ou `null` se não veio ou veio vazio. */
fun ApplicationCall.texto(nome: String): String? =
    request.queryParameters[nome]?.takeIf { it.isNotBlank() }?.let { lerTexto(nome, it) }

/**
 * Parâmetro do caminho, como o `{id}` de `/contas/{id}`. Lê só o caminho: `call.parameters` junta a consulta
 * na frente, e um `?id=` na consulta trocaria o recurso.
 */
fun RoutingCall.caminho(nome: String): String = lerTexto(nome, pathParameters.getOrFail(nome))

/** `?pagina=0&tamanho=20`, com os padrões de [PedidoDePagina] para o que não veio. */
fun ApplicationCall.pedidoDePagina() =
    PedidoDePagina(pagina = inteiro("pagina") ?: 0, tamanho = inteiro("tamanho") ?: PedidoDePagina.TAMANHO_PADRAO)

/** `?tipo=RECEITA` ou `?tipo=DESPESA`, ou `null` se não veio. */
fun ApplicationCall.tipo(nome: String): Tipo? = texto(nome)?.let { lerTipo(nome, it) }

/** `?mes=2026-09`, ou `null` se não veio. */
fun ApplicationCall.mes(nome: String): Mes? = texto(nome)?.let { valor ->
    val partes = FORMATO_DO_MES.matchEntire(valor)?.destructured
    val (ano, numero) = partes ?: throw FormatoInvalido("$nome deve estar no formato aaaa-mm, e veio \"$valor\"")
    try {
        Mes(ano.toInt(), numero.toInt())
    } catch (e: IllegalArgumentException) {
        throw FormatoInvalido("$nome deve ter o mês entre 01 e 12, e veio \"$valor\"", e)
    }
}

/** Lê `RECEITA` ou `DESPESA`. [campo] é o nome que aparece na mensagem de erro. */
fun lerTipo(campo: String, valor: String): Tipo = Tipo.entries.firstOrNull { it.name == valor }
    ?: throw FormatoInvalido("$campo deve ser RECEITA ou DESPESA, e veio \"$valor\"")

/** Lê uma data `aaaa-mm-dd` de um ano entre 1 e 9999. [campo] é o nome que aparece na mensagem de erro. */
fun lerDia(campo: String, valor: String): LocalDate {
    val dia =
        try {
            LocalDate.parse(valor)
        } catch (e: IllegalArgumentException) {
            throw FormatoInvalido("$campo deve ser uma data no formato aaaa-mm-dd, e veio \"$valor\"", e)
        }
    // O parse aceita ano com sinal e mais de quatro dígitos, como +5874898-01-01, e o PostgreSQL só guarda datas até
    // o ano 5874897. A api fica nos anos de quatro dígitos, de 1 a 9999.
    if (dia.year !in PRIMEIRO_ANO..ULTIMO_ANO) {
        throw FormatoInvalido("$campo deve ter o ano entre $PRIMEIRO_ANO e $ULTIMO_ANO, e veio \"$valor\"")
    }
    return dia
}

/**
 * Lê um texto de entrada e recusa o caractere nulo. O PostgreSQL não grava o byte 0x00 num texto, e ele chegaria ao
 * banco como erro 500. [campo] é o nome que aparece na mensagem de erro.
 */
fun lerTexto(campo: String, valor: String): String {
    if (CARACTERE_NULO in valor) throw FormatoInvalido("$campo não pode conter o caractere nulo")
    return valor
}
