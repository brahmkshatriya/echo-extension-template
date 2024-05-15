package dev.brahmkshatriya.echo.extension.network

import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class LoginInterceptor(private val client: OkHttpClient) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        return chain.proceed(
            originalRequest.newBuilder().headers(
                Headers.headersOf(
                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
                )
            ).build()
        )
    }
}