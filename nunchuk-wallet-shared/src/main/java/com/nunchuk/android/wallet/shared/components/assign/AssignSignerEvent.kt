package com.nunchuk.android.wallet.shared.components.assign

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class AssignSignerEvent {
    data class AssignSignerCompletedEvent(
        val totalRequireSigns: Int = 0,
        val masterSigners: List<MasterSigner>,
        val remoteSigners: List<SingleSigner>
    ) : AssignSignerEvent()
}

data class AssignSignerState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val selectedPFXs: List<String> = emptyList()
)