package com.nunchuk.android.widget

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.Window
import android.widget.TextView
import javax.inject.Inject

class NCInfoDialog @Inject constructor(
    private val activity: Activity
) {

    fun showDialog(
        title: String = activity.getString(R.string.nc_text_info),
        message: String,
        btnYes: String = activity.getString(R.string.nc_text_got_it),
        onYesClick: () -> Unit = {},
    ) = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setContentView(R.layout.nc_info_dialog)

        findViewById<TextView>(R.id.title).text = title
        findViewById<TextView>(R.id.btnYes).text = btnYes
        findViewById<TextView>(R.id.message).text = message
        findViewById<View>(R.id.btnYes).setOnClickListener {
            onYesClick()
            dismiss()
        }

        show()
    }

}