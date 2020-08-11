package de.bitcoin.android.domain.connect_app


import android.app.NotificationManager
import com.google.firebase.iid.FirebaseInstanceId
import de.bitcoin.android.data.model.enums.Crypto
import de.bitcoin.android.data.model.enums.OrderType
import de.bitcoin.android.data.model.local.AppConnectState
import de.bitcoin.android.data.source.order.api.OrderRepository
import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.data.source.trades.TradesRepository
import de.bitcoin.android.domain.BaseUseCase
import de.bitcoin.android.domain.model.Either
import de.bitcoin.android.domain.model.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DisconnectAppUseCase(private val notificationManager: NotificationManager,
                           private val settingsRepository: SettingsRepository,
                           private val orderRepository: OrderRepository,
                           private val tradesRepository: TradesRepository) : BaseUseCase<Boolean, DisconnectAppUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Boolean> {
        return try {
            settingsRepository.clear()

            val buyFilter = orderRepository.getFilter(OrderType.BUY).first()
            val sellFilter = orderRepository.getFilter(OrderType.SELL).first()
            orderRepository.setFilter(OrderType.BUY, buyFilter)
            orderRepository.setFilter(OrderType.SELL, sellFilter)

            settingsRepository.setActiveCrypto(Crypto.default())
            tradesRepository.setActiveCrypto(Crypto.default()[0])
            orderRepository.setActiveCrypto(OrderType.SELL, Crypto.default()[0])
            orderRepository.setActiveCrypto(OrderType.BUY, Crypto.default()[0])

            settingsRepository.setAppConnect(false)
            settingsRepository.setAppConnectState(AppConnectState.NOT_AUTHORIZED)

            withContext(Dispatchers.IO) { FirebaseInstanceId.getInstance().deleteInstanceId() }
            notificationManager.cancelAll()

            Either.Right(true)
        } catch (e: Exception) {
            Either.Left(DisconnectAppFailure(e))
        }
    }

    class Params

    data class DisconnectAppFailure(val error: Exception) : Failure.FeatureFailure(error)
}
