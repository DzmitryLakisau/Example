package de.bitcoin.android.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import de.bitcoin.android.data.model.local.AppConnectState
import de.bitcoin.android.domain.app_connect_state.GetAppConnectStateUseCase
import de.bitcoin.android.domain.common.GetNetworkStateUseCase
import de.bitcoin.android.domain.common.IsInternetAvailableUseCase
import de.bitcoin.android.presentation.base.viewmodel.ActionLiveData
import de.bitcoin.android.presentation.base.viewmodel.BaseViewModel
import de.bitcoin.android.presentation.model.Event

class AppViewModel(getNetworkStateUseCase: GetNetworkStateUseCase,
                   isInternetAvailableUseCase: IsInternetAvailableUseCase,
                   private val getAppConnectStateUseCase: GetAppConnectStateUseCase): BaseViewModel(getNetworkStateUseCase, isInternetAvailableUseCase) {

    private val _showVerifyPinScreenAction = ActionLiveData<Event<Any>>()

    val showVerifyPinScreen: LiveData<Event<Any>>
        get() = _showVerifyPinScreenAction

    fun onMoveToForeground() {
        getAppConnectStateUseCase.invoke(viewModelScope, GetAppConnectStateUseCase.Params()) { it.either({},
            { state ->
                if (state == AppConnectState.COMPLETELY_AUTHORIZED) {
                    _showVerifyPinScreenAction.value = Event(Any())
                }
            })
        }
    }
}