package com.nunchuk.android.nav.args

import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FeeSettingArgs(
    val destination: FeeSettingStartDestination,
) : Parcelable {
    fun buildBundle() = Bundle().apply {
        putInt(EXTRA_START_DESTINATION, destination.ordinal)
    }

    companion object {
        private const val EXTRA_START_DESTINATION = "EXTRA_START_DESTINATION"

        fun deserializeFrom(bundle: Bundle): FeeSettingArgs = FeeSettingArgs(
            destination = bundle.getInt(
                EXTRA_START_DESTINATION,
                FeeSettingStartDestination.MAIN.ordinal
            ).let { FeeSettingStartDestination.entries[it] }
        )
    }
}

enum class FeeSettingStartDestination {
    MAIN,
    DEFAULT_FEE_RATE,
    TAPROOT_FEE_SELECTION
}