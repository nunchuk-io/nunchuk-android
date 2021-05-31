package com.nunchuk.android.widget

import android.app.Dialog
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import android.widget.TextView
import javax.inject.Inject

class NCInputDialog @Inject constructor(private val context: Context) {

    fun showDialog(
        title: String,
        onConfirmed: (String) -> Unit = {},
        onCanceled: () -> Unit = {}
    ) {
        Dialog(context).apply {
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.nc_confirm_dialog)
            findViewById<TextView>(R.id.title).text = title
            findViewById<View>(R.id.btnYes).setOnClickListener {
                onConfirmed(findViewById<TextView>(R.id.message).text.toString())
                dismiss()
            }

            findViewById<View>(R.id.btnNo).setOnClickListener {
                onCanceled()
                dismiss()
            }
            show()
            window?.setLayout(MATCH_PARENT, MATCH_PARENT)
        }
    }
}