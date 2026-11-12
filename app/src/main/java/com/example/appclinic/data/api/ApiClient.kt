package com.example.appclinic.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // 10.0.2.2 es la dirección IP virtual del emulador que siempre apunta al localhost de la PC
    private var baseUrl: String = "http://10.0.2.2:8000/"

    private var apiService: ApiService? = null

    fun getBaseUrl(): String = baseUrl

    fun updateBaseUrl(newUrl: String) {
        var formatted = newUrl.trim()
        if (!formatted.endsWith("/")) {
            formatted += "/"
        }
        baseUrl = formatted
        apiService = null // Rebuild service on next call
    }

    fun getService(): ApiService {
        if (apiService == null) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }
        return apiService!!
    }
}
