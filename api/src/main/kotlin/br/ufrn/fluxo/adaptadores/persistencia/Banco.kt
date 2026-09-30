package br.ufrn.fluxo.adaptadores.persistencia

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import javax.sql.DataSource

private const val CONEXOES_NO_POOL = 5

/**
 * Onde está o banco. Vem de variáveis de ambiente, nunca do código. Os padrões são os do banco de
 * desenvolvimento que o `docker compose` sobe.
 */
class ConfigBanco(val url: String, val usuario: String, val senha: String) {
    companion object {
        fun doAmbiente() = ConfigBanco(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/fluxo",
            usuario = System.getenv("DB_USER") ?: "fluxo",
            senha = System.getenv("DB_PASSWORD") ?: "fluxo",
        )
    }
}

/**
 * Pool de conexões: abrir conexão é caro, e o pool reaproveita. É pequeno para caber nos 512 MB da
 * hospedagem gratuita e no limite de conexões de um banco gerenciado.
 */
fun criarDataSource(config: ConfigBanco): HikariDataSource = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = config.url
        username = config.usuario
        password = config.senha
        maximumPoolSize = CONEXOES_NO_POOL
        poolName = "fluxo"
    },
)

/**
 * Aplica as migrações pendentes de `src/main/resources/db/migration`.
 *
 * O esquema vem só das migrações: o Exposed mapeia as tabelas nos objetos `Table` deste pacote, mas
 * não cria nem altera nenhuma.
 */
fun migrar(dataSource: DataSource) {
    Flyway
        .configure()
        .dataSource(dataSource)
        .load()
        .migrate()
}
