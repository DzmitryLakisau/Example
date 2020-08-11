package de.bitcoin.android.domain.connect_app

import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.domain.BaseUseCase
import de.bitcoin.android.domain.model.Either
import de.bitcoin.android.domain.model.Failure
import java.util.*

class GetAppConnectDateUseCase(private val settingsRepository: SettingsRepository) : BaseUseCase<Date, GetAppConnectDateUseCase.Params>() {

    override suspend fun run(params: Params): Either<Failure, Date> {
        return try {
            val date = settingsRepository.getAppConnectDate()
            date?.let {
                Either.Right(date)
            } ?: Either.Left(GetAppConnectDateFailure(IllegalArgumentException("App-connect date is null")))
        } catch (e: Exception) {
            Either.Left(GetAppConnectDateFailure(e))
        }
    }

    class Params

    data class GetAppConnectDateFailure(val error: Exception) : Failure.FeatureFailure(error)
}