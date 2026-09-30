package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.aplicacao.Conflito
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException

/** SQLSTATE que o PostgreSQL devolve quando um índice único é violado. */
private const val VIOLACAO_DE_UNICIDADE = "23505"

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
