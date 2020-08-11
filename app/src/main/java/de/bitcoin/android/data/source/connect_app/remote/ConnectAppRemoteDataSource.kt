package de.bitcoin.android.data.source.connect_app.remote

import de.bitcoin.android.data.model.remote.response.BaseResponse
import de.bitcoin.android.data.model.remote.response.ConnectDeviceResponse
import de.bitcoin.android.data.model.remote.response.EncryptedConnectAppResponse
import de.bitcoin.android.data.source.connect_app.ConnectAppDataSource

class ConnectAppRemoteDataSource(private val service: ConnectAppService) : ConnectAppDataSource {

    override suspend fun connectApp(encryptedConnectAppData: String): EncryptedConnectAppResponse = service.connectApp(encryptedConnectAppData)

    override suspend fun connectDevice(deviceId: String): ConnectDeviceResponse = service.connectDevice(deviceId)

    override suspend fun setDeviceToken(token: String): BaseResponse = service.setDeviceToken(token)
}