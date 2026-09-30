package br.ufrn.fluxo

import br.ufrn.fluxo.adaptadores.persistencia.ConfigBanco
import br.ufrn.fluxo.adaptadores.persistencia.TransacoesPostgres
import br.ufrn.fluxo.adaptadores.web.ContaDto
import br.ufrn.fluxo.adaptadores.web.PaginaDeContasDto
import br.ufrn.fluxo.adaptadores.web.PaginaDeTransacoesDto
import br.ufrn.fluxo.adaptadores.web.TransacaoDto
import br.ufrn.fluxo.aplicacao.DadosDaTransacao
import br.ufrn.fluxo.aplicacao.NaoEncontrado
import br.ufrn.fluxo.dominio.Tipo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDate
import org.jetbrains.exposed.v1.jdbc.Database
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
 * Confere o que os dublês em memória não alcançam: as migrações, o SQL da paginação e dos filtros, a
 * chave estrangeira e as restrições do banco. Roda igual no Docker Desktop e no GitHub Actions, porque
 * o endereço do banco vem do container, e não de configuração.
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

    private fun corpoDaTransacao(descricao: String, centavos: Long, dia: String, tipo: String, categoria: String?) =
        """{"descricao":"$descricao","valorCentavos":$centavos,"data":"$dia","tipo":"$tipo"""" +
            categoria?.let { ""","categoria":"$it"""" }.orEmpty() + "}"

    private suspend fun HttpClient.lancar(
        contaId: String,
        descricao: String,
        centavos: Long,
        dia: String,
        tipo: String = "DESPESA",
        categoria: String? = null,
    ): HttpResponse = post("/contas/$contaId/transacoes") {
        contentType(ContentType.Application.Json)
        setBody(corpoDaTransacao(descricao, centavos, dia, tipo, categoria))
    }

    /** Conexão direta com o banco, por fora da api, para conferir o que ficou gravado. */
    private fun <T> noBanco(bloco: (Connection) -> T): T =
        DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use(bloco)

    private fun transacoesNoBanco(contaId: String): Int = noBanco { conexao ->
        conexao.prepareStatement("SELECT count(*) FROM transacoes WHERE conta_id = ?").use { consulta ->
            consulta.setString(1, contaId)
            consulta.executeQuery().use { linhas ->
                linhas.next()
                linhas.getInt(1)
            }
        }
    }

    /** Grava uma transação direto na tabela, sem passar pela validação da api. */
    private fun inserirDireto(id: String, contaId: String, centavos: Long, tipo: String) = noBanco { conexao ->
        val sql =
            "INSERT INTO transacoes (id, conta_id, descricao, valor_centavos, data, tipo) " +
                "VALUES (?, ?, 'Padaria', ?, DATE '2026-09-06', ?)"
        conexao.prepareStatement(sql).use { comando ->
            comando.setString(1, id)
            comando.setString(2, contaId)
            comando.setLong(3, centavos)
            comando.setString(4, tipo)
            comando.executeUpdate()
        }
    }

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
        assertEquals(listOf("1", "2"), versoes)
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

    @Test
    fun crudCompletoDeTransacaoDentroDaConta() = testApplication {
        val cliente = appComBanco()
        val conta = cliente.criarConta("Conta das transações").body<ContaDto>()

        val criada = cliente.lancar(conta.id, "Supermercado", 31_245, "2026-09-06", categoria = "Mercado")
        assertEquals(HttpStatusCode.Created, criada.status)
        val local = assertNotNull(criada.headers[HttpHeaders.Location])
        val id = criada.body<TransacaoDto>().id
        assertEquals("/contas/${conta.id}/transacoes/$id", local)
        val gravada = TransacaoDto(id, "Supermercado", 31_245, "2026-09-06", "DESPESA", "Mercado")
        assertEquals(gravada, cliente.get(local).body<TransacaoDto>())

        val atualizada =
            cliente.put(local) {
                contentType(ContentType.Application.Json)
                setBody(corpoDaTransacao("Devolução do mercado", 1_000, "2026-09-07", "RECEITA", categoria = null))
            }
        assertEquals(HttpStatusCode.OK, atualizada.status)
        val esperada = TransacaoDto(id, "Devolução do mercado", 1_000, "2026-09-07", "RECEITA")
        assertEquals(esperada, cliente.get(local).body<TransacaoDto>())

        assertEquals(HttpStatusCode.NoContent, cliente.delete(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.get(local).status)
        assertEquals(HttpStatusCode.NotFound, cliente.delete(local).status)
    }

    @Test
    fun transacaoPrecisaDeUmaContaQueExista() = testApplication {
        val resposta = appComBanco().lancar("conta-que-nao-existe", "Padaria", 4_590, "2026-09-06")
        assertEquals(HttpStatusCode.NotFound, resposta.status)

        // Por fora do caso de uso, quem barra é a chave estrangeira, e o repositório troca o erro do banco
        // por NaoEncontrado. É o caso da conta removida entre a conferência e a gravação.
        val banco = Database.connect(postgres.jdbcUrl, user = postgres.username, password = postgres.password)
        val dados = DadosDaTransacao("Padaria", 4_590, LocalDate(2026, 9, 6), Tipo.DESPESA)
        val erro = assertFailsWith<NaoEncontrado> { TransacoesPostgres(banco).criar("conta-que-nao-existe", dados) }
        assertEquals("conta conta-que-nao-existe não existe", erro.message)
    }

    @Test
    fun removerAContaLevaAsTransacoesJunto() = testApplication {
        val cliente = appComBanco()
        val conta = cliente.criarConta("Conta que vai embora").body<ContaDto>()
        cliente.lancar(conta.id, "Padaria", 4_590, "2026-09-06")
        cliente.lancar(conta.id, "Farmácia", 8_000, "2026-09-07")
        assertEquals(2, transacoesNoBanco(conta.id))

        assertEquals(HttpStatusCode.NoContent, cliente.delete("/contas/${conta.id}").status)
        assertEquals(0, transacoesNoBanco(conta.id))
    }

    @Test
    fun filtrosEPaginacaoDasTransacoesVaoParaOSql() = testApplication {
        val cliente = appComBanco()
        val conta = cliente.criarConta("Conta dos filtros").body<ContaDto>()
        cliente.lancar(conta.id, "Salário", 450_000, "2026-09-05", tipo = "RECEITA")
        cliente.lancar(conta.id, "Supermercado Bom Preço", 31_245, "2026-09-06", categoria = "Mercado")
        cliente.lancar(conta.id, "Supermercado de agosto", 20_000, "2026-08-31", categoria = "Mercado")
        cliente.lancar(conta.id, "Padaria 100%", 4_590, "2026-09-30")
        cliente.lancar(conta.id, "Farmácia", 8_000, "2026-09-30")

        suspend fun pagina(consulta: String) =
            cliente.get("/contas/${conta.id}/transacoes$consulta").body<PaginaDeTransacoesDto>()

        suspend fun descricoes(consulta: String) = pagina(consulta).itens.map { it.descricao }

        // Da data mais recente para a mais antiga. No mesmo dia, a gravada por último vem primeiro.
        val todas = listOf("Farmácia", "Padaria 100%", "Supermercado Bom Preço", "Salário", "Supermercado de agosto")
        assertEquals(todas, descricoes(""))

        // O mês vai do dia 1 ao último dia: 31/08 fica fora de setembro, e 30/09 entra.
        assertEquals(todas.dropLast(1), descricoes("?mes=2026-09"))
        assertEquals(listOf("Supermercado de agosto"), descricoes("?mes=2026-08"))
        assertEquals(listOf("Salário"), descricoes("?tipo=RECEITA"))
        assertEquals(listOf("Supermercado Bom Preço"), descricoes("?categoria=mercado&mes=2026-09"))
        assertEquals(listOf("Supermercado Bom Preço", "Supermercado de agosto"), descricoes("?descricao=SUPERMERCADO"))
        // O `%` digitado é texto, e não curinga.
        assertEquals(listOf("Padaria 100%"), descricoes("?descricao=%25"))

        val segunda = pagina("?tamanho=2&pagina=1")
        assertEquals(listOf("Supermercado Bom Preço", "Salário"), segunda.itens.map { it.descricao })
        assertEquals(5, segunda.total)
    }

    @Test
    fun bancoRecusaValorZeroETipoDesconhecidoMesmoPorForaDaApi() = testApplication {
        val conta = appComBanco().criarConta("Conta das restrições").body<ContaDto>()
        val valorZero = assertFailsWith<SQLException> { inserirDireto("t-zero", conta.id, 0, "DESPESA") }
        assertEquals(VIOLACAO_DE_CHECK, valorZero.sqlState)
        val tipoDesconhecido = assertFailsWith<SQLException> { inserirDireto("t-tipo", conta.id, 100, "OUTRO") }
        assertEquals(VIOLACAO_DE_CHECK, tipoDesconhecido.sqlState)
        assertEquals(1, inserirDireto("t-certa", conta.id, 100, "DESPESA"))
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
