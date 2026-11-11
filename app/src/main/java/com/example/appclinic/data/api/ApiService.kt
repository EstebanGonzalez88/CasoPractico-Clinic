package com.example.appclinic.data.api

import com.example.appclinic.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("api/expedientes")
    suspend fun crearExpediente(@Body data: ExpedienteIn): Response<ExpedienteResponse>

    @GET("api/expedientes")
    suspend fun listarExpedientes(): Response<List<ExpedienteItem>>

    @GET("api/expedientes/{id}")
    suspend fun leerExpediente(@Path("id") id: Int): Response<ExpedienteDetalle>

    @POST("api/comprobantes/{id}/firmar")
    suspend fun emitirComprobante(@Path("id") id: Int): Response<ComprobanteResponse>

    @POST("api/comprobantes/verificar")
    suspend fun verificarComprobante(@Body req: VerificacionRequest): Response<VerificacionResponse>

    @GET("api/llave-publica")
    suspend fun getLlavePublica(): Response<ResponseBody>
}
