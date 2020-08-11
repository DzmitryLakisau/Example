package de.bitcoin.android.domain.connect_app

import de.bitcoin.android.data.source.settings.SettingsRepository
import de.bitcoin.android.domain.BaseFlowUseCase
import kotlinx.coroutines.flow.Flow

class GetAppConnectUseCase(private val repository: SettingsRepository): BaseFlowUseCase<Boolean, GetAppConnectUseCase.Params>() {

    override fun execute(params: Params): Flow<Boolean> = repository.getAppConnect()

    class Params
}