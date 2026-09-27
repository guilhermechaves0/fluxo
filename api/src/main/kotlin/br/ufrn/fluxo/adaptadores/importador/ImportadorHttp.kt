package br.ufrn.fluxo.adaptadores.importador

import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ExtratoLido
import br.ufrn.fluxo.aplicacao.ImportadorDeExtratos
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
import br.ufrn.fluxo.aplicacao.LancamentoLido
import br.ufrn.fluxo.aplicacao.LinhaIgnorada
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Chama o POST /importar do importador em Go, que responde em JSON. Na Sprint 2 este adaptador dá
 * lugar ao cliente gRPC gerado de protos/, sem mudar a porta [ImportadorDeExtratos].
 */
class ImportadorHttp(private val url: String, private val cliente: HttpClient) : ImportadorDeExtratos {
    override suspend fun ler(conteudo: ByteArray): ExtratoLido {
        val resposta = enviar(conteudo)
        return when {
            resposta.status.isSuccess() -> lerCorpo(resposta).paraDominio()
            resposta.status in ERROS_DO_ARQUIVO -> throw ExtratoInvalido(detalhe(resposta))
            else -> throw ImportadorIndisponivel()
        }
    }

    private suspend fun enviar(conteudo: ByteArray): HttpResponse = try {
        cliente.post("$url/importar") {
            contentType(ContentType.Application.OctetStream)
            setBody(conteudo)
        }
    } catch (e: IOException) {
        throw ImportadorIndisponivel(e)
    }

    private suspend fun lerCorpo(resposta: HttpResponse): ExtratoJson = try {
        resposta.body()
    } catch (e: SerializationException) {
        throw ImportadorIndisponivel(e)
    }

    // O importador responde erro como application/problem+json, que o ContentNegotiation não
    // decodifica sozinho; por isso o corpo é lido como texto.
    private suspend fun detalhe(resposta: HttpResponse): String =
        runCatching { JSON.decodeFromString<ProblemaJson>(resposta.bodyAsText()).detail }.getOrNull()
            ?: "O importador recusou o arquivo."

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
        val ERROS_DO_ARQUIVO =
            setOf(HttpStatusCode.BadRequest, HttpStatusCode.UnprocessableEntity, HttpStatusCode.PayloadTooLarge)
    }
}

@Serializable
private data class ExtratoJson(
    val formato: String,
    val origem: String,
    val lancamentos: List<LancamentoJson>,
    val ignorados: List<IgnoradoJson>,
) {
    fun paraDominio() = ExtratoLido(
        formato = formato,
        origem = origem,
        lancamentos =
        lancamentos.map {
            LancamentoLido(
                idExterno = it.idExterno,
                data = LocalDate.parse(it.data),
                valorCentavos = it.valorCentavos,
                descricao = it.descricao,
                tipo = Tipo.valueOf(it.tipo),
            )
        },
        ignorados = ignorados.map { LinhaIgnorada(it.posicao, it.motivo) },
    )
}

@Serializable
private data class LancamentoJson(
    val idExterno: String,
    val data: String,
    val valorCentavos: Long,
    val descricao: String,
    val tipo: String,
)

@Serializable
private data class IgnoradoJson(val posicao: Int, val motivo: String)

@Serializable
private data class ProblemaJson(val detail: String? = null)
