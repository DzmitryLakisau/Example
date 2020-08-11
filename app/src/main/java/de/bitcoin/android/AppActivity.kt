package de.bitcoin.android

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import com.bluelinelabs.conductor.Conductor
import com.bluelinelabs.conductor.Router
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import de.bitcoin.android.data.model.enums.Crypto
import de.bitcoin.android.presentation.AppViewModel
import de.bitcoin.android.presentation.base.controller.BaseController
import de.bitcoin.android.presentation.base.extension.dpToPx
import de.bitcoin.android.presentation.base.extension.hasControllers
import de.bitcoin.android.presentation.base.extension.observeEvent
import de.bitcoin.android.presentation.base.extension.setSecure
import de.bitcoin.android.presentation.screen.alarm.PriceAlertController
import de.bitcoin.android.presentation.screen.alarm.PriceAlertController.Companion.BUNDLE_CRYPTO
import de.bitcoin.android.presentation.screen.home.HomeController
import de.bitcoin.android.presentation.screen.pin.verify.VerifyPinController
import de.bitcoin.android.presentation.screen.splash.SplashController
import org.koin.android.viewmodel.ext.android.viewModel

class AppActivity : AppCompatActivity() {

    private val viewModel: AppViewModel by viewModel()

    @BindView(R2.id.controller_container)
    lateinit var vgContainer: ViewGroup

    lateinit var router: Router

    private val appObserver: AppObserver by lazy { AppObserver(viewModel) }

    private var lastHeight = 0

    private var networkConnectionAlert: Snackbar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (App.isProd()) {
            setSecure()
        }

        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app)

        if (!isGooglePlayServicesAvailable(this)){
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        }

        ButterKnife.bind(this)

        lifecycle.apply {
            removeObserver(appObserver)
            addObserver(appObserver)
        }

        vgContainer.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect().apply { vgContainer.getWindowVisibleDisplayFrame(this) }

            val screenHeight = vgContainer.rootView.height

            // rect.bottom is the position above soft keypad or device button.
            // if keypad is shown, the rect.bottom is smaller than that before.
            val keypadHeight = screenHeight - rect.bottom - 48.dpToPx()
            if (lastHeight == keypadHeight) {
                return@addOnGlobalLayoutListener
            }

            lastHeight = keypadHeight

            val controllers = router.backstack
                .filter { transaction -> transaction.controller() is BaseController<*> }
                .map { transaction -> transaction.controller() as BaseController<*> }

            // 0.15 ratio is perhaps enough to determine keypad height.
            if (keypadHeight > screenHeight * 0.15) {
                controllers.forEach { it.onKeyboardOpened() }
            } else {
                controllers.forEach { it.onKeyboardDismissed() }
            }
        }

        router = Conductor.attachRouter(this, vgContainer, null)
        if (!router.hasRootController()) {
            router.setRoot(SplashController.newInstance())
        }

        viewModel.showVerifyPinScreen.observeEvent(this) {
            if (router.hasControllers(SplashController.TAG, VerifyPinController.TAG)) {
                return@observeEvent
            }

            router.pushController(VerifyPinController.newInstance(true))
        }

        resolveIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val crypto: Crypto? = intent?.extras?.getParcelable(BUNDLE_CRYPTO)
        val priceAlertController = PriceAlertController.newInstance(crypto)
        when {
            /** when Price Alert screen was visible or behind Pin screen we update it with a replacement */
            router.hasControllers(PriceAlertController.TAG) -> {
                val newBackStack = router.backstack
                val index = newBackStack.indexOfFirst { it.tag() == PriceAlertController.TAG }
                newBackStack[index] = priceAlertController
                router.setBackstack(newBackStack, SimpleSwapChangeHandler())
            }
            /** when Pin screen was visible with another screen behind it but not Price Alert we insert it behind Pin screen */
            router.hasControllers(SplashController.TAG, VerifyPinController.TAG) -> {
                val newBackStack = router.backstack
                newBackStack.add(router.backstackSize - 1, priceAlertController)
                router.setBackstack(newBackStack, SimpleSwapChangeHandler())
            }
            else -> router.pushController(priceAlertController)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isGooglePlayServicesAvailable(this)){
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        router.backstack.forEach { transaction -> event?.let { (transaction.controller() as? BaseController<*>)?.onDispatchTouchEvent(it) } }
        return super.dispatchTouchEvent(event)
    }

    override fun onBackPressed() {
        if (!router.handleBack()) {
            super.onBackPressed()
        }
    }

    fun close() = super.onBackPressed()

    fun showNetworkConnectionAlert() {
        if (networkConnectionAlert == null) {
            networkConnectionAlert = Snackbar.make(vgContainer, getString(R.string.no_connection), Snackbar.LENGTH_INDEFINITE)
        }

        networkConnectionAlert?.show()
    }

    fun hideNetworkConnectionAlert() {
        networkConnectionAlert?.dismiss()
    }

    private fun resolveIntent(intent: Intent) {
        val extras = intent.extras ?: return

        if (extras.containsKey(BUNDLE_CRYPTO)) {
            val backStack = listOf(
                HomeController.newInstance(),
                PriceAlertController.newInstance(extras.getParcelable(BUNDLE_CRYPTO)),
                SplashController.newInstance(true)
            )
            router.setBackstack(backStack, SimpleSwapChangeHandler())
        }
    }

    private fun isGooglePlayServicesAvailable(context: Context) = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
}
