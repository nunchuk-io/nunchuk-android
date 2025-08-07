package com.nunchuk.android.transaction.components.send.fee

import kotlinx.serialization.Serializable

@Serializable
sealed class EstimatedFeeNavigation {
    @Serializable
    data object Main : EstimatedFeeNavigation()
} 