/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.widget

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity.BOTTOM
import android.view.Gravity.FILL_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class NCToastMessage(private val activity: Activity) : DefaultLifecycleObserver {
    init {
        if (activity is AppCompatActivity) {
            activity.lifecycle.addObserver(this)
        }
    }

    private val toast = Toast(activity.applicationContext)
    private val handler = Handler(Looper.getMainLooper())
    private val dismissRunnable = Runnable {
        toast.cancel()
    }

    fun show(messageId: Int) = activity.getString(messageId).let(::showMessage)

    fun show(message: String) = message.let(::showMessage)

    @JvmOverloads
    fun showMessage(
        message: String,
        background: Int = R.drawable.nc_toast_background,
        textColor: Int = R.color.nc_black_color,
        icon: Int = R.drawable.ic_info,
        gravity: Int = BOTTOM or FILL_HORIZONTAL,
        duration: Int = Toast.LENGTH_LONG,
        offset: Int = R.dimen.nc_padding_16,
        dismissTime: Long = TIME
    ): NCToastMessage {
        val root: View = activity.layoutInflater.inflate(
            R.layout.nc_toast_message,
            activity.findViewById(R.id.custom_toast_container)
        )
        val textView: TextView = root.findViewById(R.id.text)
        textView.text = message
        textView.setTextColor(ContextCompat.getColor(activity, textColor))

        val containerView = root.findViewById<ViewGroup>(R.id.container)
        containerView.background = ResourcesCompat.getDrawable(activity.resources, background, null)

        val iconView = root.findViewById<ImageView>(R.id.icon)
        iconView.setImageResource(icon)

        val paddingVal = activity.resources.getDimension(offset).toInt()
        root.setPadding(paddingVal, paddingVal, paddingVal, paddingVal)
        toast.also {
            it.duration = duration
            it.view = root
            it.setGravity(gravity, 0, 0)
            it.show()
            handler.postDelayed(dismissRunnable, dismissTime)
        }
        return this
    }

    fun showWarning(message: String) = showMessage(
        message = message,
        background = R.drawable.nc_toast_warning_background,
        icon = R.drawable.ic_warn
    )

    fun showError(message: String) = showMessage(
        message = message,
        background = R.drawable.nc_toast_error_background,
        textColor = R.color.nc_white_color,
        icon = R.drawable.ic_info_white
    )

    fun showInfo(message: String) = showMessage(
        message = message,
        background = R.drawable.nc_toast_info_background,
        textColor = R.color.nc_fill_primary,
        icon = R.drawable.ic_info
    )

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        handler.removeCallbacks(dismissRunnable)
        toast.cancel()
    }

    companion object {
        private const val TIME = 3000L
        const val LONG_TIME = 5000L
    }

}

