package de.bitcoin.android

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import de.bitcoin.android.App.Companion.CHANNEL_ID
import de.bitcoin.android.data.model.enums.Crypto
import de.bitcoin.android.domain.connect_app.SetDeviceTokenUseCase
import de.bitcoin.android.domain.price_alert.FetchPriceAlertsUseCase
import de.bitcoin.android.presentation.screen.alarm.PriceAlertController.Companion.BUNDLE_CRYPTO
import kotlinx.coroutines.GlobalScope
import org.koin.android.ext.android.inject
import kotlin.random.Random

class NotificationService : FirebaseMessagingService() {

    private val notificationManager: NotificationManager by inject()

    private val fetchPriceAlertsUseCase: FetchPriceAlertsUseCase by inject()
    private val setDeviceTokenUseCase: SetDeviceTokenUseCase by inject()

    override fun onNewToken(token: String) = setDeviceTokenUseCase.invoke(GlobalScope, SetDeviceTokenUseCase.Params(token)) { it.either({}, {}) }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        fetchPriceAlertsUseCase.invoke(GlobalScope, FetchPriceAlertsUseCase.Params(true)) { it.either({}, {}) }

        val messageTitle = remoteMessage.data[NOTIFICATION_KEY_TITLE] ?: getString(R.string.app_name)
        val messageBody = remoteMessage.data[NOTIFICATION_KEY_BODY] ?: getString(R.string.price_alert)
        val crypto = Crypto.get(remoteMessage.data[NOTIFICATION_KEY_CRYPTO])

        val notificationIntent = Intent(this, AppActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (crypto != Crypto.UNKNOWN) {
                putExtras(Bundle().apply { putParcelable(BUNDLE_CRYPTO, crypto) })
            }
        }

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationId = Random.nextInt()

        val pendingIntent = PendingIntent.getActivity(this, notificationId, notificationIntent, PendingIntent.FLAG_ONE_SHOT)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setContentTitle(messageTitle)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSound(alarmSound)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {

        private const val NOTIFICATION_KEY_TITLE = "title"
        private const val NOTIFICATION_KEY_BODY = "body"
        private const val NOTIFICATION_KEY_CRYPTO = "currency"

    }
}