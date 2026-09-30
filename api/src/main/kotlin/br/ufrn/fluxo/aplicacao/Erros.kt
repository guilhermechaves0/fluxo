package br.ufrn.fluxo.aplicacao

/**
 * Erros que a aplicação sabe nomear. Nenhum deles fala de HTTP: quem decide que [NaoEncontrado]
 * vira 404 é o adaptador web.
 */
sealed class ErroDeAplicacao(mensagem: String, causa: Throwable? = null) : RuntimeException(mensagem, causa)

/** A entrada quebra uma ou mais regras. Traz todas as violações, e não só a primeira. */
class EntradaInvalida(val violacoes: List<String>) : ErroDeAplicacao(violacoes.joinToString("; "))

class NaoEncontrado(recurso: String, id: String, causa: Throwable? = null) :
    ErroDeAplicacao("$recurso $id não existe", causa)

/** A operação contraria o que já está gravado, como o nome de uma conta que já existe. */
class Conflito(mensagem: String, causa: Throwable? = null) : ErroDeAplicacao(mensagem, causa)
