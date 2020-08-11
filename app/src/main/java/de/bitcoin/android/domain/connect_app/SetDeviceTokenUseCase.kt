package de.bitcoin.android.domain.connect_app

import de.bitcoin.android.data.source.connect_app.ConnectAppRepository
import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.domain.BaseUseCase
import de.bitcoin.android.domain.model.Either
import de.bitcoin.android.domain.model.Failure
import kotlinx.coroutines.flow.first

class SetDeviceTokenUseCase(private val connectAppRepository: ConnectAppRepository,
                            private val settingsRepository: SettingsRepository) : BaseUseCase<Any, SetDeviceTokenUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Any> {
        return try {
            if (settingsRepository.getAppConnect().first()) {
                connectAppRepository.setDeviceToken(params.token)
            }
            Either.Right(Any())
        } catch (e: Exception) {
            Either.Left(onWrapException(e))
        }
    }

    data class Params(val token: String)
}