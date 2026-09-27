package br.ufrn.fluxo.presentation.lancamento

import br.ufrn.fluxo.dominio.Categoria
import br.ufrn.fluxo.dominio.Tipo
import br.ufrn.fluxo.dominio.Transacao
import br.ufrn.fluxo.dominio.lerData
import br.ufrn.fluxo.dominio.lerReais

private const val MAXIMO_DA_DESCRICAO = 60

/** O que foi digitado no formulário, ainda como texto. Só vira [Transacao] quando está válido. */
data class RascunhoTransacao(
    val descricao: String = "",
    val valor: String = "",
    val data: String = "",
    val tipo: Tipo = Tipo.DESPESA,
    val categoria: Categoria? = null,
)

/** Mensagem de erro de cada campo, ou `null` quando o campo está certo ou ainda vazio. */
data class ErrosDoRascunho(val descricao: String? = null, val valor: String? = null, val data: String? = null)

/**
 * Erros dos campos já preenchidos. Campo vazio não gera mensagem: o botão Salvar fica
 * desabilitado até tudo estar preenchido, e a tela mostra só a dica do campo.
 */
fun RascunhoTransacao.erros(): ErrosDoRascunho {
    val centavos = lerReais(valor)
    return ErrosDoRascunho(
        descricao =
        if (descricao.trim().length > MAXIMO_DA_DESCRICAO) "Use até $MAXIMO_DA_DESCRICAO caracteres." else null,
        valor =
        when {
            valor.isBlank() -> null
            centavos == null -> "Digite um valor como 45,90."
            centavos == 0L -> "O valor precisa ser maior que zero."
            else -> null
        },
        data = if (data.isNotBlank() && lerData(data) == null) "Use uma data válida no formato dd/mm/aaaa." else null,
    )
}

/** Troca o tipo. Receita não usa as categorias de gasto, então a categoria escolhida é limpa. */
fun RascunhoTransacao.comTipo(novo: Tipo): RascunhoTransacao =
    copy(tipo = novo, categoria = if (novo == Tipo.RECEITA) null else categoria)

/** Monta a transação, ou devolve `null` se algum campo estiver vazio ou inválido. */
fun RascunhoTransacao.paraTransacao(id: String): Transacao? {
    val centavos = lerReais(valor)?.takeIf { it > 0 }
    val dia = lerData(data)
    val descricaoLimpa = descricao.trim()
    val completo =
        centavos != null &&
            dia != null &&
            descricaoLimpa.isNotEmpty() &&
            descricaoLimpa.length <= MAXIMO_DA_DESCRICAO &&
            (tipo == Tipo.RECEITA || categoria != null)
    return if (completo) Transacao(id, descricaoLimpa, centavos, dia, tipo, categoria) else null
}

/** Verdadeiro quando o formulário pode ser salvo. Controla o botão Salvar. */
val RascunhoTransacao.podeSalvar: Boolean
    get() = paraTransacao(id = "rascunho") != null
