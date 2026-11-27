package br.ufrn.fluxo.arquitetura.violacao.aplicacao

import br.ufrn.fluxo.importador.v1.Formato
import io.grpc.Status

/** Só para o ArquiteturaTest: um "caso de uso" que conhece o gRPC e o contrato gerado. Não serve de modelo. */
class CasoDeUsoComGrpc {
    fun codigo(): Status.Code = Status.UNAVAILABLE.code

    fun formato(): Formato = Formato.FORMATO_OFX
}
