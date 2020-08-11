package de.bitcoin.android.presentation.dialog.alert

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import butterknife.BindView
import butterknife.OnClick
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import de.bitcoin.android.R
import de.bitcoin.android.R2
import de.bitcoin.android.presentation.base.extension.getString
import de.bitcoin.android.presentation.base.extension.gone
import de.bitcoin.android.presentation.base.extension.visibleOrGone

class AlertDialogController: BaseAlertDialogController {

    @BindView(R2.id.tv_alert_dialog_title)
    lateinit var tvTitle: TextView

    @BindView(R2.id.tv_alert_dialog_message)
    lateinit var tvMessage: TextView

    @BindView(R2.id.btn_positive)
    lateinit var btnOk: TextView

    @BindView(R2.id.btn_negative)
    lateinit var btnCancel: TextView

    private var listener: AlertDialogListener? = null

    constructor(args: Bundle, listener: AlertDialogListener? = null): this(args) { this.listener = listener }

    constructor(args: Bundle): super(args)

    override fun getLayoutId(): Int = R.layout.dialog_alert

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        dialog?.let { isCancelable = false }

        args.getString(BUNDLE_TITLE)?.let { tvTitle.text = it } ?: tvTitle.gone()
        tvMessage.text = args.getString(BUNDLE_MESSAGE)

        val positiveId = args.getInt(BUNDLE_POSITIVE_ACTION)
        val negativeId = args.getInt(BUNDLE_NEGATIVE_ACTION)

        val showNegative = args.getBoolean(BUNDLE_SHOW_NEGATIVE_ACTION, true)
        btnCancel.visibleOrGone(showNegative)

        btnOk.text = getString(if (positiveId != 0) positiveId else R.string.ok)
        btnCancel.text = getString(if (negativeId != 0) negativeId else R.string.cancel)
    }

    @OnClick(R2.id.btn_positive)
    fun onOkClick() {
        router.popController(this)
        listener?.onPositiveClick()
    }

    @OnClick(R2.id.btn_negative)
    fun onCancelClick() {
        router.popController(this)
        listener?.onNegativeClick()
    }

    companion object {

        const val ALERT_INTERNET_TAG = "ALERT_INTERNET_TAG"
        const val ALERT_API_TAG = "ALERT_API_TAG"
        const val ALERT_UNKNOWN_TAG = "ALERT_UNKNOWN_TAG"
        const val ALERT_ERROR_TAG = "ALERT_ERROR_TAG"
        const val ALERT_SERVER_TAG = "ALERT_SERVER_TAG"
        const val ALERT_MIN_AMOUNT_TAG = "ALERT_MIN_AMOUNT_TAG"
        const val ALERT_AMOUNT_TAG = "ALERT_AMOUNT_TAG"
        const val ALERT_PRICE_ALERT_TAG = "ALERT_PRICE_ALERT_TAG"

        private const val BUNDLE_TITLE = "BUNDLE_TITLE"
        private const val BUNDLE_MESSAGE = "BUNDLE_MESSAGE"
        private const val BUNDLE_POSITIVE_ACTION = "BUNDLE_POSITIVE"
        private const val BUNDLE_NEGATIVE_ACTION = "BUNDLE_NEGATIVE"
        private const val BUNDLE_SHOW_NEGATIVE_ACTION = "BUNDLE_SHOW_NEGATIVE_ACTION"

        fun newInstance(title: String? = null, message: String, @StringRes positive: Int? = null, @StringRes negative: Int? = null, showNegative: Boolean = true, listener: AlertDialogListener? = null): RouterTransaction {
            val args = Bundle().apply {
                title?.let { putString(BUNDLE_TITLE, title) }
                putString(BUNDLE_MESSAGE, message)
                putBoolean(BUNDLE_SHOW_NEGATIVE_ACTION, showNegative)

                positive?.let { putInt(BUNDLE_POSITIVE_ACTION, it) }
                negative?.let { putInt(BUNDLE_NEGATIVE_ACTION, it) }
            }

            return RouterTransaction.with(AlertDialogController(args, listener))
                .pushChangeHandler(SimpleSwapChangeHandler(false))
                .popChangeHandler(SimpleSwapChangeHandler())
        }
    }
}