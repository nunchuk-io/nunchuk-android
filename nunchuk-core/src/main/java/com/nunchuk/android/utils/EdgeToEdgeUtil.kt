package com.nunchuk.android.utils

import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun Toolbar.consumeEdgeToEdge() {
    // Consume the system window insets
    ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
        val systemInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
        updatePadding(top = systemInsets.top)
        WindowInsetsCompat.CONSUMED
    }
}