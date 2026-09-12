package br.ufrn.fluxo.adaptadores.web

import kotlinx.serialization.Serializable

/**
 * Corpo de erro no formato da RFC 9457 (application/problem+json).
 *
 * [type] identifica o tipo de erro e [detail] explica o caso específico.
 */
@Serializable
data class Problema(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
)
