package com.nunchuk.android.wallet.shared.components.configure

sealed class ConfigureSharedWalletEvent {
    data class ConfigureCompletedEvent(
        val totalSigns: Int,
        val requireSigns: Int,
    ) : ConfigureSharedWalletEvent()
}

data class ConfigureSharedWalletState(
    val totalSigns: Int = TOTAL_SIGNS_MIN,
    val requireSigns: Int = 0,
    val isConfigured: Boolean = false,
    val canDecreaseTotal: Boolean = false
)

const val TOTAL_SIGNS_MIN = 2