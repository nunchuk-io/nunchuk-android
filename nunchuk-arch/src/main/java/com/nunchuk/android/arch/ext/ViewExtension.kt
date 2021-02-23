package com.nunchuk.android.arch.ext

import android.view.View

var View.isViewEnabled: Boolean
    set(value) {
        isEnabled = value
        alpha = if (value) 1.0f else 0.4f
    }
    get() {
        return isEnabled && alpha == 1.0f
    }

var View.isVisible: Boolean
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }
    get() {
        return visibility == View.VISIBLE
    }

