package br.ufrn.fluxo.adaptadores.persistencia

import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DA_CATEGORIA
import br.ufrn.fluxo.dominio.TAMANHO_MAXIMO_DA_DESCRICAO
import br.ufrn.fluxo.dominio.Tipo
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.date
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

private const val TAMANHO_DO_TIPO = 7

/**
 * Mapeamento da tabela `transacoes` para o Exposed. O esquema está nas migrações (db/migration); este
 * objeto só diz ao Exposed como ler e escrever. O `criado_em` é preenchido pelo banco e só serve para
 * ordenar as transações do mesmo dia.
 */
object Transacoes : Table("transacoes") {
    val id = varchar("id", TAMANHO_DO_ID)
    val contaId = varchar("conta_id", TAMANHO_DO_ID).references(Contas.id, onDelete = ReferenceOption.CASCADE)
    val descricao = varchar("descricao", TAMANHO_MAXIMO_DA_DESCRICAO)
    val valorCentavos = long("valor_centavos")
    val data = date("data")
    val tipo = enumerationByName("tipo", TAMANHO_DO_TIPO, Tipo::class)
    val categoriaId = varchar("categoria_id", TAMANHO_MAXIMO_DA_CATEGORIA).nullable()
    val categoriaNome = varchar("categoria_nome", TAMANHO_MAXIMO_DA_CATEGORIA).nullable()
    val criadoEm = timestampWithTimeZone("criado_em")
    override val primaryKey = PrimaryKey(id)
}
