package com.nunchuk.android.wallet.assign

import com.nunchuk.android.model.SingleSigner

sealed class AssignSignerEvent {
    data class AssignSignerCompletedEvent(
        val totalRequireSigns: Int = 0,
        val selectedSigners: List<SingleSigner>
    ) : AssignSignerEvent()
}

data class AssignSignerState(
    val totalRequireSigns: Int = 0,
    val signers: List<SingleSigner> = emptyList(),
    val selectedPFXs: List<String> = emptyList()
)