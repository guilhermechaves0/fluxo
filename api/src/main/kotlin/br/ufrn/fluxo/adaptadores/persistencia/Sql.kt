package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.aplicacao.Conflito
import br.ufrn.fluxo.aplicacao.NaoEncontrado
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

// SQLSTATE que o PostgreSQL devolve quando um índice único ou uma chave estrangeira é violada.
private const val VIOLACAO_DE_UNICIDADE = "23505"
private const val VIOLACAO_DE_CHAVE_ESTRANGEIRA = "23503"

/** `%` e `_` digitados por quem busca são texto, e não curinga do LIKE. */
internal fun escaparLike(texto: String): String = texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

/**
 * Roda o [bloco] e troca a violação de índice único por [Conflito], com a [mensagem] de quem chamou.
 *
 * A regra fica no banco, e não num SELECT antes do INSERT, porque duas requisições ao mesmo tempo
 * passariam as duas pela consulta.
 */
internal suspend fun <T> traduzindoConflito(mensagem: String, bloco: suspend () -> T): T = try {
    bloco()
} catch (e: ExposedSQLException) {
    throw if (e.sqlState == VIOLACAO_DE_UNICIDADE) Conflito(mensagem, e) else e
}

/**
 * Roda o [bloco] e troca a violação da chave estrangeira por [NaoEncontrado] da conta. Cobre a conta
 * removida entre a conferência do caso de uso e a gravação, que sem isto chegaria ao cliente como 500.
 */
internal suspend fun <T> exigindoConta(contaId: String, bloco: suspend () -> T): T = try {
    bloco()
} catch (e: ExposedSQLException) {
    throw if (e.sqlState == VIOLACAO_DE_CHAVE_ESTRANGEIRA) NaoEncontrado("conta", contaId, e) else e
}
