package com.nunchuk.android.widget

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.Window
import android.widget.TextView
import javax.inject.Inject

class NCWarningDialog @Inject constructor(private val context: Context) {

    private val waningText = context.getString(R.string.nc_text_warning)
    private val btnYesText = context.getString(R.string.nc_text_yes)
    private val btnNoText = context.getString(R.string.nc_text_no)

    fun showDialog(
        title: String = waningText,
        message: String,
        btnYes: String = btnYesText,
        btnNo: String = btnNoText,
        onYesClick: () -> Unit = {},
        onNoClick: () -> Unit = {}
    ) {
        Dialog(context).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.nc_warning_dialog)

            findViewById<TextView>(R.id.message).text = message
            findViewById<View>(R.id.btnYes).setOnClickListener {
                onYesClick()
                dismiss()
            }

            findViewById<View>(R.id.btnNo).setOnClickListener {
                onNoClick()
                dismiss()
            }
            show()
        }
    }
}