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
