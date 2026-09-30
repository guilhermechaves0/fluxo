package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.PedidoDePagina
import br.ufrn.fluxo.dominio.Mes
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.util.getOrFail
import kotlinx.datetime.LocalDate

// Leitura dos parâmetros de caminho e de consulta e dos campos de texto do corpo. Tipo ou formato
// errado (`?pagina=abc`, data que não existe) é requisição malformada e responde 400. Valor bem formado
// que quebra uma regra (`?tamanho=500`) responde 422, e quem decide isso é a aplicação.

private val FORMATO_DO_MES = Regex("""(\d{4})-(\d{2})""")

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
fun ApplicationCall.texto(nome: String): String? = request.queryParameters[nome]?.takeIf { it.isNotBlank() }

/** Parâmetro do caminho, como o `{id}` de `/contas/{id}`. */
fun ApplicationCall.caminho(nome: String): String = parameters.getOrFail(nome)

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

/** Lê uma data `aaaa-mm-dd`. [campo] é o nome que aparece na mensagem de erro. */
fun lerDia(campo: String, valor: String): LocalDate = try {
    LocalDate.parse(valor)
} catch (e: IllegalArgumentException) {
    throw FormatoInvalido("$campo deve ser uma data no formato aaaa-mm-dd, e veio \"$valor\"", e)
}
