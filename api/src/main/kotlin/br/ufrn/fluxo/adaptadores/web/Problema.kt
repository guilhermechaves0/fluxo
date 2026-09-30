package br.ufrn.fluxo.adaptadores.web

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Corpo de erro no formato da RFC 9457 (application/problem+json).
 *
 * [type] identifica o tipo de erro e [detail] explica o caso específico. [violacoes] é um campo de
 * extensão, que a RFC permite: a lista de tudo o que está errado na entrada.
 */
@Serializable
data class Problema(
    val type: String,
    val title: String,
    val status: Int,
    val detail: String? = null,
    val instance: String? = null,
    val violacoes: List<String>? = null,
)

private val JSON = Json { explicitNulls = false }

/**
 * Responde o problema com o tipo de mídia da RFC, `application/problem+json`, e não com
 * `application/json`. O [Problema.instance] é o caminho da requisição.
 */
suspend fun ApplicationCall.responderProblema(
    status: HttpStatusCode,
    tipo: String,
    titulo: String,
    detalhe: String? = null,
    violacoes: List<String>? = null,
) {
    val problema =
        Problema(
            type = "https://fluxo.ufrn.br/erros/$tipo",
            title = titulo,
            status = status.value,
            detail = detalhe,
            instance = request.path(),
            violacoes = violacoes,
        )
    respondText(JSON.encodeToString(problema), ContentType.Application.ProblemJson, status)
}
