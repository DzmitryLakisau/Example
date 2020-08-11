package de.bitcoin.android.presentation.dialog

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import butterknife.BindView
import butterknife.OnClick
import butterknife.OnTextChanged
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import com.google.android.material.textfield.TextInputEditText
import de.bitcoin.android.R
import de.bitcoin.android.R2
import de.bitcoin.android.presentation.dialog.alert.BaseAlertDialogController

class PasswordDialogController : BaseAlertDialogController {

    @BindView(R2.id.ti_password)
    lateinit var tiPassword: TextInputEditText

    @BindView(R2.id.btn_positive)
    lateinit var btnOk: TextView

    @BindView(R2.id.btn_negative)
    lateinit var btnCancel: TextView

    private var callback: ((password: String?) -> Unit)? = null

    constructor() : this(Bundle.EMPTY)
    constructor(args: Bundle) : super(args)

    override fun getLayoutId(): Int = R.layout.dialog_password

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        dialog?.let { isCancelable = false }

        btnOk.isEnabled = false
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        tiPassword.requestFocus()
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
    }

    @OnClick(R2.id.btn_positive)
    fun onOkClick() {
        callback?.invoke(tiPassword.text.toString().trim())
        router.popController(this)
    }

    @OnClick(R2.id.btn_negative)
    fun onCancelClick() {
        callback?.invoke(null)
        router.popController(this)
    }

    @OnTextChanged(value = [R2.id.ti_password], callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    fun onPasswordChange(password: CharSequence) {
        btnOk.isEnabled = password.isNotEmpty()
    }

    companion object {

        private const val TAG = "PasswordDialogController"

        fun newInstance(callback: (password: String?) -> Unit): RouterTransaction =
            RouterTransaction.with(PasswordDialogController().apply { this.callback = callback })
                .pushChangeHandler(SimpleSwapChangeHandler(false))
                .popChangeHandler(SimpleSwapChangeHandler())
                .tag(TAG)
    }
}