package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.DadosDaTransacao
import br.ufrn.fluxo.aplicacao.Pagina
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.categoriaPeloNome
import kotlinx.serialization.Serializable

/**
 * Formato JSON de uma transação na api. Fica separado da entidade para que o domínio não dependa
 * de kotlinx.serialization.
 */
@Serializable
data class TransacaoDto(
    val id: String,
    val descricao: String,
    val valorCentavos: Long,
    val data: String,
    val tipo: String,
    val categoria: String? = null,
)

/**
 * Corpo do POST e do PUT de transação. Não tem `id`, que é o servidor quem atribui. [data] vem como
 * `aaaa-mm-dd`, [tipo] como `RECEITA` ou `DESPESA` e [categoria] é o nome, como "Mercado".
 */
@Serializable
data class NovaTransacaoDto(
    val descricao: String,
    val valorCentavos: Long,
    val data: String,
    val tipo: String,
    val categoria: String? = null,
)

/** Uma página da listagem. Com o [total], quem consome calcula quantas páginas existem. */
@Serializable
data class PaginaDeTransacoesDto(val itens: List<TransacaoDto>, val pagina: Int, val tamanho: Int, val total: Long)

fun Transacao.paraDto() = TransacaoDto(
    id = id,
    descricao = descricao,
    valorCentavos = valorCentavos,
    data = data.toString(),
    tipo = tipo.name,
    categoria = categoria?.nome,
)

/** Converte o corpo para os tipos do domínio. Data ou tipo fora do formato é requisição malformada (400). */
fun NovaTransacaoDto.paraDados() = DadosDaTransacao(
    descricao = descricao,
    valorCentavos = valorCentavos,
    data = lerDia("data", data),
    tipo = lerTipo("tipo", tipo),
    categoria = categoria?.takeIf { it.isNotBlank() }?.let(::categoriaPeloNome),
)

@JvmName("paginaDeTransacoesParaDto")
fun Pagina<Transacao>.paraDto() = PaginaDeTransacoesDto(
    itens = itens.map { it.paraDto() },
    pagina = pedido.pagina,
    tamanho = pedido.tamanho,
    total = total,
)
