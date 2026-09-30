package br.ufrn.fluxo.dominio

/** Tamanho máximo do nome de uma conta. É o mesmo da coluna no banco. */
const val TAMANHO_MAXIMO_DO_NOME_DA_CONTA = 60

/**
 * Regras do nome de uma conta. Devolve todas as violações de uma vez, para quem chama mostrar tudo o
 * que precisa ser corrigido. Lista vazia quer dizer nome válido.
 */
fun violacoesDaConta(nome: String): List<String> = buildList {
    if (nome.isBlank()) add("o nome da conta é obrigatório")
    if (nome.trim().length > TAMANHO_MAXIMO_DO_NOME_DA_CONTA) {
        add("o nome da conta passa de $TAMANHO_MAXIMO_DO_NOME_DA_CONTA caracteres")
    }
}

/** Tamanho máximo da descrição de uma transação. O extrato traz descrições mais longas que as digitadas no app. */
const val TAMANHO_MAXIMO_DA_DESCRICAO = 200

/** Tamanho máximo do nome de uma categoria. */
const val TAMANHO_MAXIMO_DA_CATEGORIA = 60

/**
 * Regras de uma transação, conferidas antes de montar a [Transacao]. Devolve todas as violações de uma
 * vez. Lista vazia quer dizer que os dados são válidos.
 */
fun violacoesDaTransacao(descricao: String, valorCentavos: Long, categoria: Categoria? = null): List<String> =
    buildList {
        if (descricao.isBlank()) add("a descrição é obrigatória")
        if (descricao.trim().length > TAMANHO_MAXIMO_DA_DESCRICAO) {
            add("a descrição passa de $TAMANHO_MAXIMO_DA_DESCRICAO caracteres")
        }
        if (valorCentavos <= 0) add("o valor deve ser maior que zero")
        if (categoria != null && categoria.nome.length > TAMANHO_MAXIMO_DA_CATEGORIA) {
            add("a categoria passa de $TAMANHO_MAXIMO_DA_CATEGORIA caracteres")
        }
    }
