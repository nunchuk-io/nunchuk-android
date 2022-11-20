package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class KeyPolicy(
    val autoBroadcastTransaction: Boolean = false,
    val signingDelayInHour: Int = 0,
    val spendingPolicy: SpendingPolicy? = null
) : Parcelable