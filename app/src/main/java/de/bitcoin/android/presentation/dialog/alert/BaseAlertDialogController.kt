package de.bitcoin.android.presentation.dialog.alert

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import de.bitcoin.android.presentation.base.controller.dialog.BaseDialogController
import de.bitcoin.android.presentation.base.extension.dpToPx
import de.bitcoin.android.presentation.base.extension.getWindowWidth

abstract class BaseAlertDialogController(args: Bundle): BaseDialogController(args) {

    protected var maxWidth: Int = 0

    override fun onContextAvailable(context: Context) {
        super.onContextAvailable(context)
        maxWidth = 340.dpToPx().coerceAtMost(context.getWindowWidth() - 60.dpToPx())
    }

    override fun onCreateDialog(): Dialog = super.onCreateDialog().apply { setContentView(getLayoutId()) }

    override fun setupWindow(view: View?, window: Window?) {
        super.setupWindow(view, window)

        window?.let {
            val params = WindowManager.LayoutParams()
            params.copyFrom(it.attributes)
            params.width = maxWidth
            it.attributes = params
        }
    }
}