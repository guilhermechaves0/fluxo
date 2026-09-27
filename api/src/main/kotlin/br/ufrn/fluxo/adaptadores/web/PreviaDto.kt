package br.ufrn.fluxo.adaptadores.web

import br.ufrn.fluxo.aplicacao.PreviaDeImportacao
import kotlinx.serialization.Serializable

/** Resposta do POST /importacoes/previa: as transações que o extrato geraria e as linhas descartadas. */
@Serializable
data class PreviaDto(
    val formato: String,
    val origem: String,
    val transacoes: List<TransacaoDto>,
    val ignorados: List<LinhaIgnoradaDto>,
)

@Serializable
data class LinhaIgnoradaDto(val posicao: Int, val motivo: String)

fun PreviaDeImportacao.paraDto() = PreviaDto(
    formato = formato,
    origem = origem,
    transacoes = transacoes.map { it.paraDto() },
    ignorados = ignorados.map { LinhaIgnoradaDto(it.posicao, it.motivo) },
)
