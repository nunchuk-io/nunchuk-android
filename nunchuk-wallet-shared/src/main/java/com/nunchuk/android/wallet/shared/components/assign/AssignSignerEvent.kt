package com.nunchuk.android.wallet.shared.components.assign

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class ConfigureWalletEvent {
    data class AssignSignerCompletedEvent(
        val totalRequireSigns: Int = 0,
        val masterSigners: List<MasterSigner>,
        val remoteSigners: List<SingleSigner>
    ) : ConfigureWalletEvent()
}

data class ConfigureWalletState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val selectedPFXs: List<String> = emptyList()
)