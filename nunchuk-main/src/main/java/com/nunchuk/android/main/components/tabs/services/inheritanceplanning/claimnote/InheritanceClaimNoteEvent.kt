package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claimnote

import com.nunchuk.android.model.InheritanceAdditional

sealed class InheritanceClaimNoteEvent {
    data class Loading(val isLoading: Boolean) : InheritanceClaimNoteEvent()
    data class WithdrawClick(val balance: Double) : InheritanceClaimNoteEvent()
    data class Error(val message: String) : InheritanceClaimNoteEvent()
    data class CheckHasWallet(val isHasWallet: Boolean) : InheritanceClaimNoteEvent()
}

data class InheritanceClaimNote(val inheritanceAdditional: InheritanceAdditional? = null)