package br.ufrn.fluxo.data

import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.categoriaPeloNome
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Endereço da api do Fluxo (DIM0547). No celular, `adb reverse tcp:8081 tcp:8081` faz o localhost
 * do aparelho chegar à api que roda no computador. A 8081 evita a 8080, que costuma estar ocupada.
 */
const val URL_DA_API = "http://localhost:8081"

private const val TEMPO_MAXIMO_MS = 30_000L
private val JSON = Json { ignoreUnknownKeys = true }

/** Cliente da api. Recebe o HttpClient de fora para que os testes usem o MockEngine do Ktor. */
class ApiDoFluxo(private val url: String = URL_DA_API, private val cliente: HttpClient = clientePadrao()) {
    /** Envia o extrato e devolve as transações que ele geraria. A api não grava nada nesta chamada. */
    suspend fun previaDeImportacao(nomeDoArquivo: String, conteudo: ByteArray): ExtratoNaApi {
        val resposta = enviar(nomeDoArquivo, conteudo)
        if (!resposta.status.isSuccess()) throw FalhaNaApi(detalhe(resposta))
        // SerializationException é uma IllegalArgumentException, então o catch cobre JSON fora do
        // formato e transação com dado inválido (data, tipo ou valor).
        return try {
            val corpo = JSON.decodeFromString<PreviaJson>(resposta.bodyAsText())
            ExtratoNaApi(
                origem = corpo.origem,
                transacoes = corpo.transacoes.map { it.paraTransacao() },
                ignorados = corpo.ignorados.map { it.motivo },
            )
        } catch (e: IllegalArgumentException) {
            throw FalhaNaApi("A api respondeu algo que o app não entende.", e)
        }
    }

    private suspend fun enviar(nomeDoArquivo: String, conteudo: ByteArray): HttpResponse = try {
        cliente.submitFormWithBinaryData(
            "$url/importacoes/previa",
            formData {
                append(
                    "arquivo",
                    conteudo,
                    Headers.build { append(HttpHeaders.ContentDisposition, "filename=\"$nomeDoArquivo\"") },
                )
            },
        )
    } catch (e: IOException) {
        throw FalhaNaApi("Não consegui falar com a api em $url. Ela está rodando?", e)
    }

    // Erros vêm como application/problem+json (RFC 9457); o texto do campo detail vai para a tela.
    private suspend fun detalhe(resposta: HttpResponse): String =
        runCatching { JSON.decodeFromString<ProblemaJson>(resposta.bodyAsText()).detail }.getOrNull()
            ?: "A api respondeu ${resposta.status.value}."
}

/** O que a api entendeu do extrato. [ignorados] traz o motivo de cada linha que ficou de fora. */
data class ExtratoNaApi(val origem: String, val transacoes: List<Transacao>, val ignorados: List<String>)

/** Erro de rede ou resposta de erro da api, com uma mensagem que pode ir para a tela. */
class FalhaNaApi(mensagem: String, causa: Throwable? = null) : Exception(mensagem, causa)

private fun clientePadrao() = HttpClient {
    install(ContentNegotiation) { json(JSON) }
    install(HttpTimeout) { requestTimeoutMillis = TEMPO_MAXIMO_MS }
}

@Serializable
private data class PreviaJson(
    val origem: String,
    val transacoes: List<TransacaoJson>,
    val ignorados: List<IgnoradoJson> = emptyList(),
)

@Serializable
private data class TransacaoJson(
    val id: String,
    val descricao: String,
    val valorCentavos: Long,
    val data: String,
    val tipo: String,
    val categoria: String? = null,
) {
    fun paraTransacao() = Transacao(
        id,
        descricao,
        valorCentavos,
        LocalDate.parse(data),
        Tipo.valueOf(tipo),
        categoria?.takeIf { it.isNotBlank() }?.let(::categoriaPeloNome),
    )
}

@Serializable
private data class IgnoradoJson(val posicao: Int, val motivo: String)

@Serializable
private data class ProblemaJson(val detail: String? = null)
