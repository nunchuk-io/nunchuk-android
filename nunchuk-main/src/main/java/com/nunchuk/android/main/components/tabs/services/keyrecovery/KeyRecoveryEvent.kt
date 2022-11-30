package com.nunchuk.android.main.components.tabs.services.keyrecovery

import com.nunchuk.android.main.R

sealed class KeyRecoveryEvent {
    data class Loading(val isLoading: Boolean) : KeyRecoveryEvent()
    data class ProcessFailure(val message: String) : KeyRecoveryEvent()
    data class ItemClick(val item: KeyRecoveryActionItem) : KeyRecoveryEvent()
    data class CheckPasswordSuccess(val item: KeyRecoveryActionItem, val verifyToken: String) :
        KeyRecoveryEvent()
}

data class KeyRecoveryState(
    val actionItems: List<KeyRecoveryActionItem> = arrayListOf(
        KeyRecoveryActionItem.StartKeyRecovery,
        KeyRecoveryActionItem.UpdateRecoveryQuestion
    )
)

sealed class KeyRecoveryActionItem(val title: Int) {
    object StartKeyRecovery : KeyRecoveryActionItem(R.string.nc_start_key_recovery)
    object UpdateRecoveryQuestion : KeyRecoveryActionItem(R.string.nc_update_recovery_question)
}

