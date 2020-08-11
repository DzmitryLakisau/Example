package de.bitcoin.android.data.source.connect_app

import de.bitcoin.android.data.model.remote.response.ConnectDeviceResponse
import de.bitcoin.android.data.model.remote.response.EncryptedConnectAppResponse

class RealConnectAppRepository(private val remote: ConnectAppDataSource): ConnectAppRepository {

    override suspend fun connectApp(encryptedConnectAppData: String): EncryptedConnectAppResponse = remote.connectApp(encryptedConnectAppData)

    override suspend fun connectDevice(deviceId: String): ConnectDeviceResponse = remote.connectDevice(deviceId)

    override suspend fun setDeviceToken(token: String) = remote.setDeviceToken(token)
}
