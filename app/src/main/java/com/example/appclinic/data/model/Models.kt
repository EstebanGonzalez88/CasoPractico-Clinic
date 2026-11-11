package com.example.appclinic.data.model

import com.google.gson.annotations.SerializedName

data class ExpedienteIn(
    val paciente: String,
    val diagnostico: String
)

data class ExpedienteResponse(
    val mensaje: String,
    val id: Int,
    @SerializedName("almacenado_en_bd")
    val almacenadoEnBd: String
)

data class ExpedienteItem(
    val id: Int,
    val paciente: String,
    @SerializedName("diagnostico_cifrado")
    val diagnosticoCifrado: String,
    @SerializedName("creado_en")
    val creadoEn: String? = null
)

data class ExpedienteDetalle(
    val id: Int,
    val paciente: String,
    @SerializedName("diagnostico_descifrado")
    val diagnosticoDescifrado: String,
    @SerializedName("evidencia_cifrado_bd")
    val evidenciaCifradoBd: String
)

data class ComprobanteResponse(
    val comprobante: Map<String, Any?>,
    @SerializedName("firma_digital")
    val firmaDigital: String,
    val algoritmo: String
)

data class VerificacionRequest(
    val payload: Map<String, Any?>,
    @SerializedName("firma_hex")
    val firmaHex: String
)

data class VerificacionResponse(
    val valido: Boolean,
    val mensaje: String
)
