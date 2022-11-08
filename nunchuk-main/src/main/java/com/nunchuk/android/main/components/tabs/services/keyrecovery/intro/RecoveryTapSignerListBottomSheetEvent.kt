package com.nunchuk.android.main.components.tabs.services.keyrecovery.intro

sealed class RecoveryTapSignerListBottomSheetEvent {
    object ContinueClick : RecoveryTapSignerListBottomSheetEvent()
}

data class RecoveryTapSignerListBottomSheetState(val selectedSignerId: String? = null)