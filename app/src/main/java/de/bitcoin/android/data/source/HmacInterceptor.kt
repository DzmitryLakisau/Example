package de.bitcoin.android.data.source

import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.presentation.base.extension.md5
import de.bitcoin.android.presentation.base.extension.toHexString
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HmacInterceptor(private val settingsRepository: SettingsRepository) : Interceptor {

    private var latestNonce: Long = 0

    private val lock: Any = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        if (isPublicApi(request)) {
            return chain.proceed(request)
        }

        synchronized(lock) {
            val newRequest = runBlocking {
                val apiKey = settingsRepository.getApiKey() ?: ""
                val apiSecret = settingsRepository.getApiSecret() ?: ""

                var nonce = System.currentTimeMillis()
                if (nonce <= latestNonce)
                    nonce = ++latestNonce
                else
                    latestNonce = nonce

                val md5 = when {
                    request.method == "POST" && request.body != null -> generateMd5FromPostParameters(request.body!!)
                    else -> generateMd5("").toHexString()
                }

                val hmacString = "${request.method}#${request.url}#$apiKey#$nonce#$md5"
                val hmac = hmacSha265(apiSecret, hmacString)

                request.newBuilder()
                    .header("X-API-KEY", apiKey)
                    .header("X-API-NONCE", nonce.toString())
                    .header("X-API-SIGNATURE", hmac)
                    .method(request.method, request.body)
                    .build()
            }

            return chain.proceed(newRequest)
        }
    }

    private fun generateMd5(input: String): ByteArray = input.md5()

    //body must be in FormUrlEncoded, not in Json
    private fun generateMd5FromPostParameters(body: RequestBody): String {
        val buffer = Buffer()
        body.writeTo(buffer)

        //parameters must be in alphabetical order by key
        val unorderedString = buffer.readUtf8()
        val orderedString = unorderedString.split("&").sorted().joinToString("&")

        return generateMd5(orderedString).toHexString()
    }

    private fun hmacSha265(key: String, data: String): String {
        if (key.isEmpty()) return ""

        val algorithm = "HmacSHA256"
        val sha256Hmac = Mac.getInstance(algorithm)
        val secretKey = SecretKeySpec(key.toByteArray(), algorithm)
        sha256Hmac.init(secretKey)
        val resultBytes = sha256Hmac.doFinal(data.toByteArray())
        return resultBytes.toHexString()
    }

    //public api doesn't require signing
    private fun isPublicApi(request: Request): Boolean {
        return request.url.toString().endsWith("/rates")
    }
}
