package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.web.PaginaDeTransacoesDto
import br.ufrn.fluxo.adaptadores.web.Problema
import br.ufrn.fluxo.adaptadores.web.TransacaoDto
import br.ufrn.fluxo.dominio.Conta
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * As rotas das transações com as portas em memória: o que é HTTP (status, cabeçalhos, filtros e
 * problem details) sem banco e sem Docker. As contas `c-1` e `c-2` já existem em todo teste.
 */
class RotasDeTransacoesTest {
    private fun ApplicationTestBuilder.app(): HttpClient {
        val contas = ContasEmMemoria(Conta("c-1", "Nubank"), Conta("c-2", "Carteira"))
        application { configurar(modulosEmMemoria(contas = contas)) }
        return createClient { install(ContentNegotiation) { json() } }
    }

    private suspend fun HttpClient.enviar(metodo: HttpMethod, caminho: String, corpo: String) = request(caminho) {
        method = metodo
        contentType(ContentType.Application.Json)
        setBody(corpo)
    }

    private suspend fun HttpClient.lancar(
        descricao: String,
        centavos: Long,
        dia: String,
        tipo: String = "DESPESA",
        categoria: String? = null,
        conta: String = "c-1",
    ): HttpResponse {
        val campoDaCategoria = categoria?.let { ""","categoria":"$it"""" }.orEmpty()
        val corpo =
            """{"descricao":"$descricao","valorCentavos":$centavos,"data":"$dia","tipo":"$tipo"$campoDaCategoria}"""
        return enviar(HttpMethod.Post, "/contas/$conta/transacoes", corpo)
    }

    private suspend fun HttpClient.descricoes(consulta: String): List<String> =
        get("/contas/c-1/transacoes$consulta").body<PaginaDeTransacoesDto>().itens.map { it.descricao }

    private suspend fun HttpResponse.problema(): Problema {
        assertEquals(ContentType.Application.ProblemJson, contentType()?.withoutParameters())
        return Json.decodeFromString<Problema>(bodyAsText())
    }

    @Test
    fun criarDevolve201ComLocationDentroDaConta() = testApplication {
        val cliente = app()
        val resposta = cliente.lancar("Supermercado", 31_245, "2026-09-06", categoria = "Mercado")
        assertEquals(HttpStatusCode.Created, resposta.status)
        val criada = resposta.body<TransacaoDto>()
        assertEquals("/contas/c-1/transacoes/${criada.id}", resposta.headers[HttpHeaders.Location])
        assertEquals("Mercado", criada.categoria)
        val local = assertNotNull(resposta.headers[HttpHeaders.Location])
        assertEquals(criada, cliente.get(local).body<TransacaoDto>())
    }

    @Test
    fun dadosInvalidosDevolvem422ComTodasAsViolacoes() = testApplication {
        val resposta = app().lancar("   ", 0, "2026-09-06")
        assertEquals(HttpStatusCode.UnprocessableEntity, resposta.status)
        val violacoes = resposta.problema().violacoes
        assertEquals(listOf("a descrição é obrigatória", "o valor deve ser maior que zero"), violacoes)
    }

    @Test
    fun dataOuTipoForaDoFormatoDevolvem400() = testApplication {
        val cliente = app()
        val dataQueNaoExiste = cliente.lancar("Padaria", 4_590, "2026-02-31")
        assertEquals(HttpStatusCode.BadRequest, dataQueNaoExiste.status)
        val problema = dataQueNaoExiste.problema()
        assertEquals("https://fluxo.ufrn.br/erros/requisicao-malformada", problema.type)
        assertEquals("data deve ser uma data no formato aaaa-mm-dd, e veio \"2026-02-31\"", problema.detail)
        assertEquals(HttpStatusCode.BadRequest, cliente.lancar("Padaria", 4_590, "2026-09-06", tipo = "OUTRO").status)
        val semValor = cliente.enviar(HttpMethod.Post, "/contas/c-1/transacoes", """{"descricao":"Padaria"}""")
        assertEquals(HttpStatusCode.BadRequest, semValor.status)
    }

    @Test
    fun contaInexistenteDevolve404DaConta() = testApplication {
        val cliente = app()
        val listagem = cliente.get("/contas/c-99/transacoes")
        assertEquals(HttpStatusCode.NotFound, listagem.status)
        assertEquals("conta c-99 não existe", listagem.problema().detail)
        assertEquals(HttpStatusCode.NotFound, cliente.lancar("Padaria", 4_590, "2026-09-06", conta = "c-99").status)
    }

    @Test
    fun transacaoDeOutraContaDevolve404() = testApplication {
        val cliente = app()
        val criada = cliente.lancar("Padaria", 4_590, "2026-09-06").body<TransacaoDto>()
        assertEquals(HttpStatusCode.OK, cliente.get("/contas/c-1/transacoes/${criada.id}").status)
        assertEquals(HttpStatusCode.NotFound, cliente.get("/contas/c-2/transacoes/${criada.id}").status)
        assertEquals(HttpStatusCode.NotFound, cliente.delete("/contas/c-2/transacoes/${criada.id}").status)
    }

    @Test
    fun listagemFiltraPorTipoMesCategoriaEDescricao() = testApplication {
        val cliente = app()
        cliente.lancar("Salário", 450_000, "2026-09-05", tipo = "RECEITA")
        cliente.lancar("Supermercado", 31_245, "2026-09-06", categoria = "Mercado")
        cliente.lancar("Supermercado de agosto", 20_000, "2026-08-20", categoria = "Mercado")
        cliente.lancar("Padaria", 4_590, "2026-09-10")

        assertEquals(listOf("Salário"), cliente.descricoes("?tipo=RECEITA"))
        assertEquals(listOf("Supermercado de agosto"), cliente.descricoes("?mes=2026-08"))
        assertEquals(listOf("Supermercado"), cliente.descricoes("?mes=2026-09&categoria=mercado"))
        assertEquals(listOf("Padaria"), cliente.descricoes("?descricao=pada"))
        assertEquals(listOf("Padaria", "Supermercado", "Salário"), cliente.descricoes("?mes=2026-09"))
    }

    @Test
    fun listagemPaginaDaMaisRecenteParaAMaisAntiga() = testApplication {
        val cliente = app()
        cliente.lancar("Aluguel", 150_000, "2026-09-05")
        cliente.lancar("Supermercado", 31_245, "2026-09-06")
        cliente.lancar("Padaria", 4_590, "2026-09-10")

        val primeira = cliente.get("/contas/c-1/transacoes?tamanho=2").body<PaginaDeTransacoesDto>()
        assertEquals(listOf("Padaria", "Supermercado"), primeira.itens.map { it.descricao })
        assertEquals(3, primeira.total)
        assertEquals(listOf("Aluguel"), cliente.descricoes("?tamanho=2&pagina=1"))
    }

    @Test
    fun filtroComFormatoErradoDevolve400() = testApplication {
        val cliente = app()
        assertEquals(HttpStatusCode.BadRequest, cliente.get("/contas/c-1/transacoes?mes=2026-9").status)
        val mesQueNaoExiste = cliente.get("/contas/c-1/transacoes?mes=2026-13")
        assertEquals(HttpStatusCode.BadRequest, mesQueNaoExiste.status)
        assertEquals("mes deve ter o mês entre 01 e 12, e veio \"2026-13\"", mesQueNaoExiste.problema().detail)
        val tipoErrado = cliente.get("/contas/c-1/transacoes?tipo=gasto")
        assertEquals(HttpStatusCode.BadRequest, tipoErrado.status)
        assertEquals("tipo deve ser RECEITA ou DESPESA, e veio \"gasto\"", tipoErrado.problema().detail)
    }

    @Test
    fun atualizarSubstituiOsDadosERemoverDevolve204() = testApplication {
        val cliente = app()
        val criada = cliente.lancar("Padaria", 4_590, "2026-09-06").body<TransacaoDto>()
        val caminho = "/contas/c-1/transacoes/${criada.id}"

        val corpo = """{"descricao":"Padaria da esquina","valorCentavos":5000,"data":"2026-09-07","tipo":"DESPESA"}"""
        val atualizada = cliente.enviar(HttpMethod.Put, caminho, corpo)
        assertEquals(HttpStatusCode.OK, atualizada.status)
        val esperada = TransacaoDto(criada.id, "Padaria da esquina", 5_000, "2026-09-07", "DESPESA")
        assertEquals(esperada, atualizada.body<TransacaoDto>())

        assertEquals(HttpStatusCode.NoContent, cliente.delete(caminho).status)
        assertEquals(HttpStatusCode.NotFound, cliente.get(caminho).status)
        assertEquals(HttpStatusCode.NotFound, cliente.enviar(HttpMethod.Put, caminho, corpo).status)
    }
}
