package com.skywatch.cg.data.siros

/**
 * Modelo que representa um registro bruto do CSV do SIROS/ANAC.
 * Todos os campos são Strings opcionais pois o CSV pode ter campos vazios.
 */
data class SirosRecord(
    val codigoEmpresa: String?,
    val empresa: String?,
    val numeroVoo: String?,
    val equipamento: String?,
    val seg: String?,
    val ter: String?,
    val qua: String?,
    val qui: String?,
    val sex: String?,
    val sab: String?,
    val dom: String?,
    val quantidadeAssentos: String?,
    val numeroSiros: String?,
    val situacaoSiros: String?,
    val dataRegistro: String?,
    val inicioOperacao: String?,
    val fimOperacao: String?,
    val naturezaOperacao: String?,
    val numeroEtapa: String?,
    val codigoOrigem: String?,
    val arptOrigem: String?,
    val codigoDestino: String?,
    val arptDestino: String?,
    val horarioPartida: String?,
    val horarioChegada: String?,
    val tipoServico: String?,
    val objetoTransporte: String?,
    val codeshare: String?
) {
    /**
     * Verifica se o registro tem dados mínimos para ser processado.
     */
    fun isValid(): Boolean {
        return codigoEmpresa?.isNotBlank() == true &&
               empresa?.isNotBlank() == true &&
               numeroVoo?.isNotBlank() == true &&
               codigoOrigem?.isNotBlank() == true &&
               codigoDestino?.isNotBlank() == true
    }
}