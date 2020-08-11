package de.bitcoin.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON

open class App: Application() {

    override fun onCreate() {
        super.onCreate()

        System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)

        if (!BuildConfig.DEBUG) {
            Fabric.with(this, Crashlytics())
        }

        createNotificationChannelIfNeeded()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channelId = CHANNEL_ID
        val channelName = getString(R.string.price_alarm)
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT))
    }

    companion object {

        const val CHANNEL_ID = "price_alarm"

        fun isProd(): Boolean = BuildConfig.FLAVOR == "prod"
        fun isQA(): Boolean = BuildConfig.FLAVOR == "qa"
    }
}