package com.nunchuk.android.wallet.util

import androidx.compose.ui.graphics.Color

internal fun String.hexToColor(): Color {
    return Color(android.graphics.Color.parseColor(this))
}