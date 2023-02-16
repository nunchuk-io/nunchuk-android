/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.widget.util

import android.app.Activity
import android.graphics.Color
import android.view.View.*
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.core.view.WindowInsetsControllerCompat

@Suppress("DEPRECATION")
fun Activity.setTransparentStatusBar(useDarkTheme: Boolean = true) {
    if (!useDarkTheme) {
        window.decorView.systemUiVisibility = (SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                or SYSTEM_UI_FLAG_LAYOUT_STABLE
                or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        window.statusBarColor = Color.TRANSPARENT
    } else {
        window.setFlags(FLAG_LAYOUT_NO_LIMITS, FLAG_LAYOUT_NO_LIMITS)
    }
}

@Suppress("DEPRECATION")
fun Activity.setLightStatusBar(light: Boolean = true) {
    window.apply {
        statusBarColor = Color.WHITE
        navigationBarColor =  Color.WHITE
    }
    WindowInsetsControllerCompat(window, window.decorView).apply {
        isAppearanceLightStatusBars = light
        isAppearanceLightNavigationBars = light
    }
}

