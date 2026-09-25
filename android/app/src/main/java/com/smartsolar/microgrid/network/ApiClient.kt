package com.smartsolar.microgrid.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.smartsolar.microgrid.BuildConfig

object ApiClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = TokenManager.getToken()
        
        if (token != null) {
            val newRequest = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor { chain ->
            // Scanned credentials and verified personal details must never appear in HTTP logs.
            val path = chain.request().url.encodedPath
            if (path.endsWith("/verify-qr") || path.contains("/reservations")) chain.proceed(chain.request())
            else loggingInterceptor.intercept(chain)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
