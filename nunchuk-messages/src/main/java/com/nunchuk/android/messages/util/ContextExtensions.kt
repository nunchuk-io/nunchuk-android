package com.nunchuk.android.messages.util

import android.view.View
import androidx.core.content.ContextCompat
import com.nunchuk.android.messages.R

fun View.bindNotificationBackground(highlight: Boolean) {
    background = if (highlight) {
        ContextCompat.getDrawable(context, R.drawable.nc_slime_tint_background)
    } else {
        ContextCompat.getDrawable(context, R.drawable.nc_rounded_whisper_disable_background)
    }
}