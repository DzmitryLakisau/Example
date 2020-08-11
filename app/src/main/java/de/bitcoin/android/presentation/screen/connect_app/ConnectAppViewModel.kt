package de.bitcoin.android.presentation.screen.connect_app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import de.bitcoin.android.data.model.remote.response.Code
import de.bitcoin.android.domain.common.GetNetworkStateUseCase
import de.bitcoin.android.domain.common.IsInternetAvailableUseCase
import de.bitcoin.android.domain.common.IsQRCodeValidUseCase
import de.bitcoin.android.domain.connect_app.*
import de.bitcoin.android.domain.model.ApiFailure
import de.bitcoin.android.domain.model.Failure
import de.bitcoin.android.domain.model.NetworkFailure
import de.bitcoin.android.domain.model.UnknownFailure
import de.bitcoin.android.presentation.base.extension.debounce
import de.bitcoin.android.presentation.base.viewmodel.ActionLiveData
import de.bitcoin.android.presentation.base.viewmodel.BaseViewModel
import de.bitcoin.android.presentation.model.Event
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*

class ConnectAppViewModel(getNetworkStateUseCase: GetNetworkStateUseCase,
                          isInternetAvailableUseCase: IsInternetAvailableUseCase,
                          private val getAppConnectUseCase: GetAppConnectUseCase,
                          private val context: Context,
                          private val connectAppUseCase: ConnectAppUseCase,
                          private val disconnectAppUseCase: DisconnectAppUseCase,
                          private val getAppConnectDateUseCase: GetAppConnectDateUseCase,
                          private val getUsernameUseCase: GetUsernameUseCase,
                          private val isQRCodeValidUseCase: IsQRCodeValidUseCase): BaseViewModel(getNetworkStateUseCase, isInternetAvailableUseCase) {

    private lateinit var qrCode: String

    private val _showDisconnectDialogAction = ActionLiveData<Event<Any>>()
    private val _showPasswordDialogAction = ActionLiveData<Event<Any>>()
    private val _showPinDialogAction = ActionLiveData<Event<Any>>()
    private val _showCameraPermissionDialogAction = ActionLiveData<Event<Any>>()
    private val _showNoInternetConnectionDialogWithCallbackAction = ActionLiveData<Event<Any>>()
    private val _showErrorDialogWithCallbackAction = ActionLiveData<Event<Code>>()

    private val _showAppConnectedLiveData = MutableLiveData<Boolean>()
    private val _usernameLiveData = MutableLiveData<String>()
    private val _showCameraLiveData = MutableLiveData<Boolean>()
    private val _isDialogDisplayedLiveData = MutableLiveData<Boolean>()
    private val _appConnectDateLiveData = MutableLiveData<Date>()

    val showDisconnectDialogAction: LiveData<Event<Any>>
        get() = _showDisconnectDialogAction

    val showPasswordDialogAction: LiveData<Event<Any>>
        get() = _showPasswordDialogAction

    val showPinDialogAction: LiveData<Event<Any>>
        get() = _showPinDialogAction

    val showAppConnected: LiveData<Boolean>
        get() = _showAppConnectedLiveData

    val username: LiveData<String>
        get() = _usernameLiveData

    val showCamera: LiveData<Boolean>
        get() = _showCameraLiveData

    val isDialogDisplayed: LiveData<Boolean>
        get() = _isDialogDisplayedLiveData

    val showCameraPermissionDialogAction: LiveData<Event<Any>>
        get() = _showCameraPermissionDialogAction

    val appConnectDate: LiveData<Date>
        get() = _appConnectDateLiveData

    val showErrorDialogWithCallback: LiveData<Event<Code>>
        get() = _showErrorDialogWithCallbackAction

    val showNoInternetConnectionDialogWithCallback: LiveData<Event<Any>>
        get() = _showNoInternetConnectionDialogWithCallbackAction.debounce(250)

    init {
        _showAppConnectedLiveData.value = false
        _isDialogDisplayedLiveData.value = false
    }

    override fun getSoftInputMode(): Int = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING

    override fun onHandleRequestPermissions(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onHandleRequestPermissions(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_ID_CAMERA) {
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            _showCameraLiveData.value = true
        }
    }

    fun startAppConnect() {
        viewModelScope.launch {
            val connected = getAppConnectUseCase.execute(GetAppConnectUseCase.Params()).first()
            if (connected) {
                getUsernameUseCase.invoke(viewModelScope, GetUsernameUseCase.Params()) { it.either({}, ::onUsernameSuccess) }
                getAppConnectDateUseCase.invoke(viewModelScope, GetAppConnectDateUseCase.Params()) { it.either({}, ::onAppConnectDateSuccess) }
                _showCameraLiveData.value = false
            } else {
                delay(400)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                    _showCameraPermissionDialogAction.value = Event(Any())
                } else {
                    _showCameraLiveData.value = true
                }
            }
        }
    }

    fun onPasswordEnter(password: String?) {
        if (password == null) {
            _isDialogDisplayedLiveData.value = false
            startAppConnect()
            return
        }

        _showKeyboardAction.value = Event(false)
        _showProgressLiveData.value = true
        connectAppUseCase.invoke(viewModelScope, ConnectAppUseCase.Params(qrCode, password)) { it.either(::onFailure, ::onConnectAppSuccess) }
    }

    fun onQrScanned(qrCode: String) {
        isQRCodeValidUseCase.invoke(viewModelScope, IsQRCodeValidUseCase.Params(qrCode)) {
            it.either(
                {
                    {
                        _isDialogDisplayedLiveData.value = false
                        _showCameraLiveData.value = true
                    }
                },
                { valid ->
                    if (valid) {
                        this.qrCode = qrCode
                        _isDialogDisplayedLiveData.value = true
                        _showPasswordDialogAction.value = Event(Any())
                    } else {
                        _isDialogDisplayedLiveData.value = false
                        _showCameraLiveData.value = true
                    }
                })
        }
    }

    fun onDisconnectClick() = disconnectAppUseCase.invoke(viewModelScope, DisconnectAppUseCase.Params()) { it.either({}, ::onDisconnectAppSuccess) }

    fun onDialogDismiss() {
        _isDialogDisplayedLiveData.value = false
        startAppConnect()
    }

    private fun onFailure(failure: Failure) {
        _showProgressLiveData.value = false

        when (failure) {
            is NetworkFailure -> _showNoInternetConnectionDialogWithCallbackAction.value = Event(Any())
            is UnknownFailure -> _showErrorDialogWithCallbackAction.value = Event(Code(-1))
            is ApiFailure -> _showErrorDialogWithCallbackAction.value = Event(failure.first?.code ?: Code(-1))
        }
    }

    private fun onUsernameSuccess(username: String) {
        _usernameLiveData.value = username
        _showAppConnectedLiveData.value = true
    }

    private fun onAppConnectDateSuccess(date: Date) {
        _appConnectDateLiveData.value = date
    }

    private fun onConnectAppSuccess(result: Any) {
        _showProgressLiveData.value = false
        _showPinDialogAction.value = Event(Any())
    }

    private fun onDisconnectAppSuccess(result: Boolean) {
        _showAppConnectedLiveData.value = false
        _isDialogDisplayedLiveData.value = true
        _showDisconnectDialogAction.value = Event(Any())
    }

    companion object {

        const val REQUEST_ID_CAMERA = 1
    }
}
