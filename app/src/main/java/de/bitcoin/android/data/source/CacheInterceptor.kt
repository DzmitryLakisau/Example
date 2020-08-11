package de.bitcoin.android.data.source

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

class CacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val cacheControl = CacheControl.Builder()
            .maxAge(10, TimeUnit.SECONDS)
            .build()

        //set cache header for server
        val newRequest = request.newBuilder()
            .removeHeader("Pragma")
            .removeHeader("Cache-Control")
            .header("Cache-Control", cacheControl.toString())
            .build()

        val response = chain.proceed(newRequest)

        //set cache header for local cache
        return response.newBuilder()
            .header("Cache-Control", cacheControl.toString())
            .build()
    }
}
