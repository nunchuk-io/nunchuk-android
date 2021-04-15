package com.nunchuk.android.widget

import android.app.Activity
import android.view.Gravity.BOTTOM
import android.view.Gravity.FILL_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.nunchuk.android.widget.util.NCCountdownTimer

class NCToastMessage constructor(private val activity: Activity) {

    private val timer = NCCountdownTimer()

    fun show(messageId: Int) {
        val message = activity.getString(messageId)
        showMessage(message)
    }

    @JvmOverloads
    fun showMessage(
        message: String,
        background: Int = R.drawable.nc_toast_background,
        icon: Int = R.drawable.ic_info,
        gravity: Int = BOTTOM or FILL_HORIZONTAL,
        duration: Int = Toast.LENGTH_LONG,
        offset: Int = R.dimen.nc_padding_16
    ) {
        val root: View = activity.layoutInflater.inflate(
            R.layout.nc_toast_message,
            activity.findViewById(R.id.custom_toast_container) as ViewGroup?
        )
        root.findViewById<TextView>(R.id.text).text = message

        val containerView = root.findViewById<ViewGroup>(R.id.container)
        containerView.background = ResourcesCompat.getDrawable(activity.resources, background, null)

        val iconView = root.findViewById<ImageView>(R.id.icon)
        iconView.setImageResource(icon)

        val paddingVal = activity.resources.getDimension(offset).toInt()
        root.setPadding(paddingVal, paddingVal, paddingVal, paddingVal)
        Toast(activity.applicationContext).also {
            it.setGravity(gravity, 0, 0)
            it.duration = duration
            it.view = root
            it.show()
            timer.doAfter(it::cancel)
        }
    }

    fun showWarning(message: String) {
        showMessage(
            message = message,
            background = R.drawable.nc_toast_warning_background,
            icon = R.drawable.ic_warn
        )
    }

}

