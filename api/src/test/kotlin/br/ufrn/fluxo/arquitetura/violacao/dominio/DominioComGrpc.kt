package br.ufrn.fluxo.arquitetura.violacao.dominio

import io.grpc.Status

/** Só para o ArquiteturaTest: um "domínio" que importa o gRPC de propósito. Não serve de modelo. */
class DominioComGrpc {
    fun codigo(): Status.Code = Status.INVALID_ARGUMENT.code
}
