package de.bitcoin.android.data.source.connect_app

import de.bitcoin.android.data.model.remote.response.BaseResponse
import de.bitcoin.android.data.model.remote.response.ConnectDeviceResponse
import de.bitcoin.android.data.model.remote.response.EncryptedConnectAppResponse

interface ConnectAppRepository {

    suspend fun connectApp(encryptedConnectAppData: String): EncryptedConnectAppResponse

    suspend fun connectDevice(deviceId: String): ConnectDeviceResponse

    suspend fun setDeviceToken(token: String): BaseResponse
}
