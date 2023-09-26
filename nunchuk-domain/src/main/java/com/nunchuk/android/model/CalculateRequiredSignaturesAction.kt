package com.nunchuk.android.model

import androidx.annotation.Keep


@Keep
enum class CalculateRequiredSignaturesAction {
    CREATE_OR_UPDATE, CANCEL, REQUEST_PLANNING
}