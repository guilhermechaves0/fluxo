package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.ArquivoGrandeDemais
import br.ufrn.fluxo.aplicacao.Conflito
import br.ufrn.fluxo.aplicacao.EntradaInvalida
import br.ufrn.fluxo.aplicacao.ExtratoInvalido
import br.ufrn.fluxo.aplicacao.ImportadorIndisponivel
import br.ufrn.fluxo.aplicacao.NaoEncontrado
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.statuspages.StatusPagesConfig

/**
 * Exceção vira resposta num lugar só. Os casos de uso lançam erros com nome, e é aqui que cada nome
 * ganha um status HTTP, sempre em problem details (RFC 9457).
 *
 * | Erro                                        | Status |
 * |---------------------------------------------|--------|
 * | JSON malformado ou parâmetro de tipo errado | 400    |
 * | NaoEncontrado                               | 404    |
 * | Conflito                                    | 409    |
 * | ArquivoGrandeDemais                         | 413    |
 * | EntradaInvalida, ExtratoInvalido            | 422    |
 * | ImportadorIndisponivel                      | 503    |
 * | qualquer outro                              | 500    |
 */
fun Application.tratarErros() {
    install(StatusPages) {
        errosDaEntrada()
        errosDaImportacao()
        exception<Throwable> { call, causa ->
            // O detalhe fica só no log: a resposta não mostra o que aconteceu por dentro.
            call.application.environment.log
                .error("erro não tratado", causa)
            call.responderProblema(HttpStatusCode.InternalServerError, "interno", "Erro interno")
        }
    }
}

private fun StatusPagesConfig.errosDaEntrada() {
    exception<BadRequestException> { call, causa ->
        val detalhe = causa.cause?.message ?: causa.message
        call.responderProblema(HttpStatusCode.BadRequest, "requisicao-malformada", "Requisição malformada", detalhe)
    }
    exception<EntradaInvalida> { call, causa ->
        val status = HttpStatusCode.UnprocessableEntity
        call.responderProblema(status, "entrada-invalida", "Entrada inválida", causa.message, causa.violacoes)
    }
    // O `require` do domínio e dos casos de uso também é entrada inválida.
    exception<IllegalArgumentException> { call, causa ->
        val status = HttpStatusCode.UnprocessableEntity
        call.responderProblema(status, "entrada-invalida", "Entrada inválida", causa.message)
    }
    exception<NaoEncontrado> { call, causa ->
        call.responderProblema(HttpStatusCode.NotFound, "nao-encontrado", "Recurso inexistente", causa.message)
    }
    exception<Conflito> { call, causa ->
        call.responderProblema(HttpStatusCode.Conflict, "conflito", "Conflito com o que já está gravado", causa.message)
    }
}

private fun StatusPagesConfig.errosDaImportacao() {
    exception<ExtratoInvalido> { call, causa ->
        val status = HttpStatusCode.UnprocessableEntity
        call.responderProblema(status, "extrato-nao-reconhecido", "Extrato não reconhecido", causa.message)
    }
    exception<ArquivoGrandeDemais> { call, causa ->
        val status = HttpStatusCode.PayloadTooLarge
        call.responderProblema(status, "extrato-grande-demais", "Extrato grande demais", causa.message)
    }
    exception<ImportadorIndisponivel> { call, causa ->
        call.application.environment.log
            .warn("importador indisponível: ${causa.cause?.message ?: causa.message}")
        val status = HttpStatusCode.ServiceUnavailable
        call.responderProblema(status, "importador-indisponivel", "Importador indisponível", causa.message)
    }
}
