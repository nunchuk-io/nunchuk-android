package com.nunchuk.android.widget

import android.app.Activity
import android.view.Gravity.BOTTOM
import android.view.Gravity.FILL_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.nunchuk.android.widget.util.NunchukCountdownTimer

class NCToastMessage constructor(private val activity: Activity) {

    private val timer = NunchukCountdownTimer()

    fun show(
        messageId: Int,
        gravity: Int = BOTTOM or FILL_HORIZONTAL,
        duration: Int = Toast.LENGTH_LONG,
        offset: Int = R.dimen.nc_padding_16
    ) {
        val container: View = activity.layoutInflater.inflate(
            R.layout.nc_toast,
            activity.findViewById(R.id.custom_toast_container) as ViewGroup?
        )
        container.findViewById<TextView>(R.id.text).text = activity.getString(messageId)
        val paddingVal = activity.resources.getDimension(offset).toInt()
        container.setPadding(paddingVal, paddingVal, paddingVal, paddingVal)
        Toast(activity.applicationContext).also {
            it.setGravity(gravity, 0, 0)
            it.duration = duration
            it.view = container
            it.show()
            timer.doAfter(it::cancel)
        }
    }

}

