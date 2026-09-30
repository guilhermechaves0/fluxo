package br.ufrn.fluxo.arquitetura.violacao.dominio

import io.ktor.http.HttpStatusCode

/** Só para o ArquiteturaTest: um "domínio" que importa Ktor de propósito. Não serve de modelo. */
class DominioContaminado {
    fun status(): HttpStatusCode = HttpStatusCode.OK
}
