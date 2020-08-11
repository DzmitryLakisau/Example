package de.bitcoin.android.data.source.connect_app.remote

import de.bitcoin.android.data.model.remote.response.BaseResponse
import de.bitcoin.android.data.model.remote.response.ConnectDeviceResponse
import de.bitcoin.android.data.model.remote.response.EncryptedConnectAppResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ConnectAppService {

    @FormUrlEncoded
    @POST("app/connect")
    suspend fun connectApp(@Field("connect_data") encryptedConnectAppData: String): EncryptedConnectAppResponse

    @FormUrlEncoded
    @POST("v4/app/connect/device")
    suspend fun connectDevice(@Field("device_id") deviceId: String, @Field("device_os") deviceOs: String = "android"): ConnectDeviceResponse

    @FormUrlEncoded
    @POST("v4/app/pushToken")
    suspend fun setDeviceToken(@Field("push_token") pushToken: String): BaseResponse
}
