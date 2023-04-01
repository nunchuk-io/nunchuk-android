package com.nunchuk.android.core.util

import androidx.compose.ui.graphics.Color

fun String.hexToColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}