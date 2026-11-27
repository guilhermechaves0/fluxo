package br.ufrn.fluxo

/** O que o executável da api faz, conforme o primeiro argumento. */
enum class Comando {
    /** Sobe o servidor HTTP (aplicando as migrações antes, como sempre). */
    SERVIR,

    /** Só aplica as migrações do Flyway e termina, para o CI e a hospedagem sem `preDeployCommand`. */
    MIGRAR,
}

/** Sem argumento a api serve; `migrar` aplica as migrações. Qualquer outra palavra é erro de uso. */
fun escolherComando(args: Array<String>): Comando = when (val nome = args.firstOrNull()) {
    null, "servir" -> Comando.SERVIR
    "migrar" -> Comando.MIGRAR
    else -> throw IllegalArgumentException("Comando desconhecido: $nome. Use `migrar` ou nenhum argumento.")
}
