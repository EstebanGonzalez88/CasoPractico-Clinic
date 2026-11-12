package com.example.appclinic.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
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
                .addInterceptor { chain ->
                    val originalRequest = chain.request()
                    try {
                        chain.proceed(originalRequest)
                    } catch (e: Exception) {
                        val urlString = originalRequest.url.toString()
                        val fallbackUrl = when {
                            urlString.contains("127.0.0.1") -> urlString.replace("127.0.0.1", "10.0.2.2")
                            urlString.contains("10.0.2.2") -> urlString.replace("10.0.2.2", "127.0.0.1")
                            else -> null
                        }
                        if (fallbackUrl != null) {
                            val newRequest = originalRequest.newBuilder()
                                .url(fallbackUrl)
                                .build()
                            chain.proceed(newRequest)
                        } else {
                            throw e
                        }
                    }
                }
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
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
