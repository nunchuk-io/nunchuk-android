package com.nunchuk.android.widget

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.Window
import javax.inject.Inject

class NCLoadingDialogCreator @Inject constructor(val activity: Activity) {

    fun showDialog() = Dialog(activity).apply {
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setContentView(R.layout.nc_loading_dialog)
        show()
        window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

}
