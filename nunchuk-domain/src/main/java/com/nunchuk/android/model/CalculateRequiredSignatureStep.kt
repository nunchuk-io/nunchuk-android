package com.nunchuk.android.model

import androidx.annotation.Keep

@Keep
enum class CalculateRequiredSignatureStep {
    REQUEST_RECOVER, PENDING_APPROVAL, RECOVER
}