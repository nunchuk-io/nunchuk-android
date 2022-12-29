package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class KeyPolicy(
    val autoBroadcastTransaction: Boolean = false,
    val signingDelayInSeconds: Int = 0,
    val spendingPolicy: SpendingPolicy? = null
) : Parcelable {

    companion object {
        const val ONE_HOUR_TO_SECONDS = 60 * 60
        const val ONE_MINUTE_TO_SECONDS = 60
    }

    fun getSigningDelayInHours() = signingDelayInSeconds / ONE_HOUR_TO_SECONDS
    fun getSigningDelayInMinutes() =  (signingDelayInSeconds % ONE_HOUR_TO_SECONDS) / ONE_MINUTE_TO_SECONDS
}