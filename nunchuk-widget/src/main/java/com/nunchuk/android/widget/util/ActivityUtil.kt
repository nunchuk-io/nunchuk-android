package com.nunchuk.android.widget.util

import android.app.Activity
import android.view.View
import android.view.WindowManager

@Suppress("DEPRECATION")
fun Activity.setTransparentStatusBar(useDarkTheme: Boolean = true) {
    if (!useDarkTheme) {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        window.statusBarColor = 0x000000
    } else {
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }
}