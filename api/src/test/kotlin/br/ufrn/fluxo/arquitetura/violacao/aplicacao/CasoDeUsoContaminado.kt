package br.ufrn.fluxo.arquitetura.violacao.aplicacao

import io.ktor.http.HttpStatusCode

/** Só para o ArquiteturaTest: um "caso de uso" que decide o status HTTP. Não serve de modelo. */
class CasoDeUsoContaminado {
    fun status(): HttpStatusCode = HttpStatusCode.NotFound
}
