package com.nunchuk.android.widget.util

import android.app.Activity
import android.os.Build
import android.view.View.*
import android.view.WindowManager.LayoutParams
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.core.content.ContextCompat
import com.nunchuk.android.widget.R

@Suppress("DEPRECATION")
fun Activity.setTransparentStatusBar(useDarkTheme: Boolean = true) {
    if (!useDarkTheme) {
        window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        window.statusBarColor = 0x000000
    } else {
        window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
    }
}

@Suppress("DEPRECATION")
fun Activity.setLightStatusBar() {
    val attributes: LayoutParams = window.attributes
    attributes.flags = attributes.flags and LayoutParams.FLAG_TRANSLUCENT_STATUS.inv()
    window.attributes = attributes
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    } else {
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    window.statusBarColor = ContextCompat.getColor(this, R.color.nc_white_color)
}

