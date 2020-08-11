package de.bitcoin.android.domain.connect_app


import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.Gson
import de.bitcoin.android.BuildConfig
import de.bitcoin.android.data.model.QrCode
import de.bitcoin.android.data.model.enums.Crypto
import de.bitcoin.android.data.model.enums.OrderType
import de.bitcoin.android.data.model.local.AppConnectState
import de.bitcoin.android.data.model.remote.request.ConnectAppRequest
import de.bitcoin.android.data.model.remote.response.ConnectAppResponse
import de.bitcoin.android.data.source.account.AccountRepository
import de.bitcoin.android.data.source.connect_app.ConnectAppRepository
import de.bitcoin.android.data.source.order.api.OrderRepository
import de.bitcoin.android.data.source.price_alert.PriceAlertRepository
import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.domain.BaseUseCase
import de.bitcoin.android.domain.model.Either
import de.bitcoin.android.domain.model.Failure
import de.bitcoin.android.presentation.base.extension.toBase64String
import de.bitcoin.android.presentation.base.extension.toBytes
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

class ConnectAppUseCase(private val gson: Gson,
                        private val connectAppRepository: ConnectAppRepository,
                        private val settingsRepository: SettingsRepository,
                        private val accountRepository: AccountRepository,
                        private val orderRepository: OrderRepository,
                        private val priceAlertRepository: PriceAlertRepository) : BaseUseCase<Boolean, ConnectAppUseCase.Params>() {

    private val nonce = Random.Default.nextBytes(24).toBase64String()

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        return try {
            val deviceId = Random.Default.nextBytes(48).toBase64String() //TODO from where to get it?

            val connectHash = gson.fromJson(params.qrCode, QrCode::class.java).connectHash

            val jsonToEncrypt = gson.toJson(ConnectAppRequest(nonce, params.password, connectHash))

            val encryptedConnectAppData = encryptAsymmetrically(jsonToEncrypt)

            val connectData = connectAppRepository.connectApp(encryptedConnectAppData).connectData

            val decryptedJson = decryptSymmetrically(connectData)

            val connectAppResponse = gson.fromJson(decryptedJson, ConnectAppResponse::class.java)

            settingsRepository.setApiKey(connectAppResponse.apiKey)
            settingsRepository.setApiSecret(connectAppResponse.apiSecret)

            val connectDeviceResponse = connectAppRepository.connectDevice(deviceId)

            val isAppConnected = decryptSymmetrically(connectDeviceResponse.status)?.contains("OK", true) ?: false

            if (isAppConnected) {
                val token = getFirebaseToken()
                connectAppRepository.setDeviceToken(token)

                val data = accountRepository.getAccountConnect(true)
                settingsRepository.setActiveCrypto(data.crypto)

                val fCrypto = data.crypto.firstOrNull() ?: Crypto.BTC
                orderRepository.setActiveCrypto(OrderType.BUY, fCrypto)
                orderRepository.setActiveCrypto(OrderType.SELL, fCrypto)

                priceAlertRepository.getPriceAlertsData(true)
            }

            settingsRepository.setAppConnect(isAppConnected)
            settingsRepository.setAppConnectState(AppConnectState.APP_CONNECTED)

            Either.Right(isAppConnected)
        } catch (e: Exception) {
            Either.Left(onWrapException(e))
        }
    }

    private fun encryptAsymmetrically(input: String): String {
        val publicKeyBytes = BuildConfig.PUBLIC_KEY.toBytes()
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance(RSA_ALGORITHM)
        val pubKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("$RSA_ALGORITHM/$RSA_BLOCK_MODE/$RSA_PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, pubKey)
        val encrypted = cipher.doFinal(input.toByteArray())

        return encrypted.toBase64String()
    }

    /** Input Vector is prefixed to the encrypted data */
    private fun decryptSymmetrically(input: String): String? {
        val data = input.toBytes()

        val cipher = Cipher.getInstance("$AES_ALGORITHM/$AES_BLOCK_MODE/$AES_PADDING")

        val inputVectorBytes = data.copyOfRange(0, cipher.blockSize)
        val dataBytes = data.copyOfRange(cipher.blockSize, data.size)

        val keySpec = SecretKeySpec(nonce.toByteArray(), AES_ALGORITHM)

        val inputVectorParameterSpec = IvParameterSpec(inputVectorBytes)

        cipher.init(Cipher.DECRYPT_MODE, keySpec, inputVectorParameterSpec)
        val bytes = cipher.doFinal(dataBytes)

        return String(bytes)
    }

    private suspend fun getFirebaseToken(): String {
        return suspendCoroutine { continuation ->
            FirebaseInstanceId.getInstance().instanceId
                .addOnSuccessListener {
                    continuation.resume(it.token)
                }
                .addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    data class Params(val qrCode: String, val password: String)

    companion object {
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_BLOCK_MODE = "ECB"
        private const val RSA_PADDING = "PKCS1PADDING"
        private const val AES_ALGORITHM = "AES"
        private const val AES_BLOCK_MODE = "CBC"
        private const val AES_PADDING = "NOPADDING"
    }
}
