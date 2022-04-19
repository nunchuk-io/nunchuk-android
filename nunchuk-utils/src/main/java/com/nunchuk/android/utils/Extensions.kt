package com.nunchuk.android.utils

import android.widget.EditText
import kotlin.math.roundToInt

fun CharSequence?.safeManualFee() = try {
    if (isNullOrEmpty()) 0 else (toString().toDouble() * 1000).roundToInt()
} catch (t: Throwable) {
    CrashlyticsReporter.recordException(t)
    0
}

fun CharSequence?.isNoneEmpty() = this?.toString().orEmpty().isNotEmpty()

fun EditText?.getTrimmedText() = this?.text?.trim().toString()
