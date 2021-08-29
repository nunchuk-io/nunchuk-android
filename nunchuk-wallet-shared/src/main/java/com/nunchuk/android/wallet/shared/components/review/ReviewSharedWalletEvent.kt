package com.nunchuk.android.wallet.shared.components.review

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class ReviewSharedWalletEvent {
    data class InitWalletErrorEvent(val message: String) : ReviewSharedWalletEvent()
    object InitWalletCompletedEvent : ReviewSharedWalletEvent()
}

data class ReviewSharedWalletState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val selectedPFXs: List<String> = emptyList()
)