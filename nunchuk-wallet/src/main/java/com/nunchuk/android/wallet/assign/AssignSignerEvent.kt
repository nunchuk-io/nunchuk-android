package com.nunchuk.android.wallet.assign

import com.nunchuk.android.model.SingleSigner

sealed class AssignSignerEvent {
    data class AssignSignerCompletedEvent(
        val requiredSignersNumber: Int = 0,
        val signers: List<SingleSigner>
    ) : AssignSignerEvent()
}

data class AssignSignerState(
    val requiredSignersNumber: Int = 0,
    val signers: List<SingleSigner> = emptyList(),
    val selectedPFXs: List<String> = emptyList()
)