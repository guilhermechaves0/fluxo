package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DO_NOME_DA_CONTA
import org.jetbrains.exposed.v1.core.Table

/** Tamanho de um UUID escrito como texto, o formato dos ids. */
internal const val TAMANHO_DO_ID = 36

/**
 * Mapeamento da tabela `contas` para o Exposed. O esquema está nas migrações (db/migration); este
 * objeto só diz ao Exposed como ler e escrever.
 */
object Contas : Table("contas") {
    val id = varchar("id", TAMANHO_DO_ID)
    val nome = varchar("nome", TAMANHO_MAXIMO_DO_NOME_DA_CONTA)
    override val primaryKey = PrimaryKey(id)
}
