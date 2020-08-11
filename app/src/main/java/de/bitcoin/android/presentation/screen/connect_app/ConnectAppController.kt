package de.bitcoin.android.presentation.screen.connect_app

import android.Manifest
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.Group
import butterknife.BindView
import butterknife.OnClick
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.VerticalChangeHandler
import com.budiyev.android.codescanner.*
import com.google.zxing.BarcodeFormat
import de.bitcoin.android.R
import de.bitcoin.android.R2
import de.bitcoin.android.presentation.base.controller.BaseController
import de.bitcoin.android.presentation.base.extension.*
import de.bitcoin.android.presentation.dialog.PasswordDialogController
import de.bitcoin.android.presentation.dialog.ProgressDialogController
import de.bitcoin.android.presentation.dialog.alert.AlertDialogController
import de.bitcoin.android.presentation.dialog.alert.AlertDialogListener
import de.bitcoin.android.presentation.screen.pin.create.CreatePinController

class ConnectAppController : BaseController<ConnectAppViewModel> {

    override val viewModel: ConnectAppViewModel by viewModel()

    @BindView(R2.id.toolbar)
    lateinit var tbToolbar: Toolbar

    @BindView(R2.id.frameLayout_camera_preview)
    lateinit var frameLayoutCorners: FrameLayout

    @BindView(R2.id.surfaceView_camera_preview)
    lateinit var cameraPreview: CodeScannerView

    @BindView(R2.id.textView_username)
    lateinit var textViewUsername: TextView

    @BindView(R2.id.textView_connected)
    lateinit var textViewConnected: TextView

    @BindView(R2.id.textView_disconnected)
    lateinit var textViewDisconnected: TextView

    @BindView(R2.id.group_views_when_connected)
    lateinit var groupViewsWhenConnected: Group

    private lateinit var codeScanner: CodeScanner

    constructor() : super()
    constructor(args: Bundle) : super(args)

    override val closeDialogOnDetach: Boolean = false

    override fun getLayoutId(): Int = R.layout.controller_connect_app

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        codeScanner = CodeScanner(getContext(), cameraPreview).apply {
            formats = listOf(BarcodeFormat.QR_CODE)
            camera = CodeScanner.CAMERA_BACK
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            decodeCallback = DecodeCallback { activity?.runOnUiThread { viewModel.onQrScanned(it.text) }  }
            errorCallback = ErrorCallback { }
        }

        tbToolbar.setNavigationOnClickListener {
            viewModel.onHandleBack()

            if (!router.handleBack()) {
                activity?.finish()
            }
        }

        viewModel.showProgress.observeNonNull(this) { show ->
            if (show) {
                showProgress()
            } else {
                hideProgress()
            }
        }

        viewModel.showPasswordDialogAction.observeEvent(this) {
            router.pushController(PasswordDialogController.newInstance { password -> viewModel.onPasswordEnter(password) })
        }

        viewModel.showPinDialogAction.observeEvent(this) {
            showPinDisclaimerDialog()
        }

        viewModel.showAppConnected.observeNonNull(this) { connected ->
            if (connected) {
                frameLayoutCorners.setBackgroundResource(R.drawable.bg_cornered_orange)
                groupViewsWhenConnected.visible()
                textViewDisconnected.gone()
            }
            else {
                frameLayoutCorners.setBackgroundResource(R.drawable.bg_cornered_grey)
                groupViewsWhenConnected.gone()
                textViewDisconnected.visible()
            }
        }

        viewModel.username.observeNonNull(this) { username ->
            textViewUsername.text = username
        }

        viewModel.appConnectDate.observeNonNull(this) {
            textViewConnected.text = getString(R.string.connected_since, it.toDDMMYYYY())
        }

        viewModel.showDisconnectDialogAction.observeEvent(this) {
            showDisconnectDialog()
        }

        viewModel.showCameraPermissionDialogAction.observeEvent(this) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), ConnectAppViewModel.REQUEST_ID_CAMERA)
        }

        viewModel.showNoInternetConnectionDialogWithCallback.observeEvent(this) {
            if (getRootRouter().hasControllers(AlertDialogController.ALERT_INTERNET_TAG)) {
                return@observeEvent
            }

            getRootRouter().pushController(AlertDialogController
                    .newInstance(getString(R.string.error), getString(R.string.no_internet_connection), showNegative = false, listener = object : AlertDialogListener {
                        override fun onPositiveClick() {
                            viewModel.onDialogDismiss()
                        }
                    })
                    .tag(AlertDialogController.ALERT_INTERNET_TAG))
        }

        viewModel.showErrorDialogWithCallback.observeEvent(this) { code ->
            if (router.hasControllers(AlertDialogController.ALERT_ERROR_TAG)) {
                return@observeEvent
            }

            router.pushController(AlertDialogController
                .newInstance(getString(R.string.error), code.toUserFriendlyString(getContext()), showNegative = false, listener = object : AlertDialogListener {
                    override fun onPositiveClick() {
                        viewModel.onDialogDismiss()
                    }
                }).tag(AlertDialogController.ALERT_ERROR_TAG)
            )
        }

        viewModel.showCamera.observeNonNull(this) { show ->
            if (show) {
                if (viewModel.isDialogDisplayed.value == false) {
                    codeScanner.startPreview()
                }
                cameraPreview.visible()
            } else {
                codeScanner.releaseResources()
                cameraPreview.gone()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) = viewModel.onHandleRequestPermissions(requestCode, permissions, grantResults)

    override fun onAttach(view: View) {
        super.onAttach(view)
        viewModel.startAppConnect()
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        codeScanner.releaseResources()
    }

    private fun showDisconnectDialog() = router.pushController(AlertDialogController.newInstance(
        message = getString(R.string.app_was_disconnected), showNegative = false, listener = object : AlertDialogListener {
            override fun onPositiveClick() {
                viewModel.onDialogDismiss()
            }
        })
    )

    private fun showPinDisclaimerDialog() = router.pushController(AlertDialogController.newInstance(
        message = getString(R.string.enter_pin_disclaimer), positive = R.string.cont, showNegative = false, listener = object : AlertDialogListener {
            override fun onPositiveClick() {
                router.replaceTopController(CreatePinController.newInstance())
            }
        })
    )

    private fun showProgress() {
        if (getRootRouter().hasControllers(ProgressDialogController.TAG)) {
            return
        }

        getRootRouter().pushController(ProgressDialogController.newInstance())
    }

    private fun hideProgress() {
        getRootRouter().getControllerWithTag(ProgressDialogController.TAG)?.let {
            getRootRouter().popController(it)
        }
    }


    @OnClick(R2.id.button_disconnect_app)
    fun onDisconnectClick() = viewModel.onDisconnectClick()

    companion object {

        private const val TAG = "ConnectAppController"

        fun newInstance(): RouterTransaction {
            return RouterTransaction.with(ConnectAppController())
                .popChangeHandler(VerticalChangeHandler())
                .pushChangeHandler(VerticalChangeHandler())
                .tag(TAG)
        }
    }
}
