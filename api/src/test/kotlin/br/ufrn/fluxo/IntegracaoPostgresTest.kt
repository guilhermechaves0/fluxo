package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.persistencia.ConfigBanco
import br.ufrn.fluxo.adaptadores.web.ContaDto
import br.ufrn.fluxo.adaptadores.web.PaginaDeContasDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.testcontainers.postgresql.PostgreSQLContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * A api inteira contra um PostgreSQL de verdade, num container descartável.
 *
 * Confere o que os dublês em memória não alcançam: as migrações, o SQL da paginação e do filtro e as
 * restrições do banco. Roda igual no Docker Desktop e no GitHub Actions, porque o endereço do banco
 * vem do container, e não de configuração.
 */
class IntegracaoPostgresTest {
    private fun ApplicationTestBuilder.appComBanco(): HttpClient {
        application { modulo(urlImportador = "http://localhost:9090", configBanco = config) }
        return createClient { install(ContentNegotiation) { json() } }
    }

    private suspend fun HttpClient.criarConta(nome: String) = post("/contas") {
        contentType(ContentType.Application.Json)
        setBody("""{"nome":"$nome"}""")
    }

    private suspend fun HttpClient.renomearConta(caminho: String, nome: String) = put(caminho) {
        contentType(ContentType.Application.Json)
        setBody("""{"nome":"$nome"}""")
    }

    /** Conexão direta com o banco, por fora da api, para conferir o que ficou gravado. */
    private fun <T> noBanco(bloco: (Connection) -> T): T =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use(bloco)

    @Test
    fun migracoesCriamOEsquemaERodarDeNovoNaoMudaNada() {
        // Cada testApplication sobe a api do zero e chama o Flyway. Na segunda vez não há o que aplicar.
        repeat(2) {
            testApplication { assertEquals(HttpStatusCode.OK, appComBanco().get("/health").status) }
        }
        val versoes =
            noBanco { conexao ->
                val consulta = "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank"
                conexao.createStatement().executeQuery(consulta).use { linhas ->
                    buildList { while (linhas.next()) add(linhas.getString(1)) }
                }
            }
        assertEquals(listOf("1"), versoes)
    }

    @Test
    fun crudCompletoDeConta() = testApplication {
        val cliente = appComBanco()
        val criada = cliente.criarConta("Conta do CRUD")
        assertEquals(HttpStatusCode.Created, criada.status)
        val local = assertNotNull(criada.headers[HttpHeaders.Location])
        assertEquals("Conta do CRUD", cliente.get(local).body<ContaDto>().nome)

        val renomeada = cliente.renomearConta(local, "Conta renomeada")
        assertEquals(HttpStatusCode.OK, renomeada.status)
        assertEquals("Conta renomeada", cliente.get(local).body<ContaDto>().nome)

        assertEquals(HttpStatusCode.NoContent, cliente.delete(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.get(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.delete(local).status)
    }

    @Test
    fun nomeRepetidoEhConflitoNoIndiceUnicoSemDiferenciarMaiusculas() = testApplication {
        val cliente = appComBanco()
        assertEquals(HttpStatusCode.Created, cliente.criarConta("Conta Repetida").status)

        val repetida = cliente.criarConta("CONTA REPETIDA")
        assertEquals(HttpStatusCode.Conflict, repetida.status)
        assertEquals(ContentType.Application.ProblemJson, repetida.contentType()?.withoutParameters())

        val outra = cliente.criarConta("Conta Livre").body<ContaDto>()
        assertEquals(HttpStatusCode.Conflict, cliente.renomearConta("/contas/${outra.id}", "conta repetida").status)
    }

    @Test
    fun paginacaoEFiltroVaoParaOSql() = testApplication {
        val cliente = appComBanco()
        listOf("Pag Charlie", "Pag alfa", "Pag Bravo").forEach { cliente.criarConta(it) }

        val primeira = cliente.get("/contas?nome=pag&tamanho=2").body<PaginaDeContasDto>()
        assertEquals(listOf("Pag alfa", "Pag Bravo"), primeira.itens.map { it.nome })
        assertEquals(3, primeira.total)

        val segunda = cliente.get("/contas?nome=PAG&tamanho=2&pagina=1").body<PaginaDeContasDto>()
        assertEquals(listOf("Pag Charlie"), segunda.itens.map { it.nome })

        // O `%` digitado é texto, e não curinga: nenhuma conta tem `%` no nome.
        assertEquals(0, cliente.get("/contas?nome=%25").body<PaginaDeContasDto>().total)
    }

    @Test
    fun bancoRecusaNomeVazioMesmoPorForaDaApi() {
        testApplication { appComBanco().get("/health") }
        val erro =
            assertFailsWith<SQLException> {
                noBanco { it.createStatement().executeUpdate("INSERT INTO contas (id, nome) VALUES ('direto', '   ')") }
            }
        assertEquals(VIOLACAO_DE_CHECK, erro.sqlState)
    }

    private companion object {
        /** SQLSTATE do PostgreSQL para uma restrição CHECK violada. */
        const val VIOLACAO_DE_CHECK = "23514"

        // Um banco para a classe inteira. Cada teste cria as próprias contas, com nomes que os outros
        // testes não usam.
        val postgres = PostgreSQLContainer("postgres:17-alpine").apply { start() }
        val config = ConfigBanco(postgres.jdbcUrl, postgres.username, postgres.password)
    }
}
