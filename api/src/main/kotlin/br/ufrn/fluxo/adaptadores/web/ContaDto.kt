package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.dominio.Conta
import kotlinx.serialization.Serializable

/** Formato JSON de uma conta na api. */
@Serializable
data class ContaDto(val id: String, val nome: String)

/** Corpo do POST e do PUT de conta. Não tem `id`, que é o servidor quem atribui. */
@Serializable
data class NovaContaDto(val nome: String)

/** Uma página da listagem. Com o [total], quem consome calcula quantas páginas existem. */
@Serializable
data class PaginaDeContasDto(val itens: List<ContaDto>, val pagina: Int, val tamanho: Int, val total: Long)

fun Conta.paraDto() = ContaDto(id = id, nome = nome)

fun Pagina<Conta>.paraDto() = PaginaDeContasDto(
    itens = itens.map { it.paraDto() },
    pagina = pedido.pagina,
    tamanho = pedido.tamanho,
    total = total,
)
