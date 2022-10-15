package com.nunchuk.android.wallet.shared.components.assign

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class AssignSignerEvent {
    object ChangeBip32Success : AssignSignerEvent()
    data class Loading(val isLoading: Boolean) : AssignSignerEvent()
    data class ShowError(val message: String) : AssignSignerEvent()
    data class AssignSignerCompletedEvent(
        val roomId: String
    ) : AssignSignerEvent()
}

data class AssignSignerState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val masterSignerMap: Map<String, SingleSigner> = emptyMap(),
    val selectedSigner: Set<SignerModel> = setOf(),
    val filterRecSigners: List<SingleSigner> = emptyList()
)