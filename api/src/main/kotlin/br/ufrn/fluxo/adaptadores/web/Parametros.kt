package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.PedidoDePagina
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.util.getOrFail

// Leitura dos parâmetros de caminho e de consulta. Tipo errado (`?pagina=abc`) é requisição
// malformada e responde 400. Valor bem formado que quebra uma regra (`?tamanho=500`) responde 422, e
// quem decide isso é a aplicação.

/** Parâmetro de consulta inteiro, ou `null` se não veio. */
fun ApplicationCall.inteiro(nome: String): Int? = request.queryParameters[nome]?.let {
    it.toIntOrNull() ?: throw BadRequestException("$nome deve ser um número inteiro, e veio \"$it\"")
}

/** Parâmetro de consulta de texto, ou `null` se não veio ou veio vazio. */
fun ApplicationCall.texto(nome: String): String? = request.queryParameters[nome]?.takeIf { it.isNotBlank() }

/** Parâmetro do caminho, como o `{id}` de `/contas/{id}`. */
fun ApplicationCall.caminho(nome: String): String = parameters.getOrFail(nome)

/** `?pagina=0&tamanho=20`, com os padrões de [PedidoDePagina] para o que não veio. */
fun ApplicationCall.pedidoDePagina() =
    PedidoDePagina(pagina = inteiro("pagina") ?: 0, tamanho = inteiro("tamanho") ?: PedidoDePagina.TAMANHO_PADRAO)
