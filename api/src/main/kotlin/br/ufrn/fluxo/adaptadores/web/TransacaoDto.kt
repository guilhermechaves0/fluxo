package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.dominio.Transacao
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

fun Transacao.paraDto() = TransacaoDto(
    id = id,
    descricao = descricao,
    valorCentavos = valorCentavos,
    data = data.toString(),
    tipo = tipo.name,
    categoria = categoria?.nome,
)
