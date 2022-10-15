package com.nunchuk.android.wallet.components.configure

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class ConfigureWalletEvent {
    object ChangeBip32Success : ConfigureWalletEvent()
    data class Loading(val loading: Boolean) : ConfigureWalletEvent()
    data class AssignSignerCompletedEvent(
        val totalRequireSigns: Int = 0,
        val masterSigners: List<SingleSigner>,
        val remoteSigners: List<SingleSigner>
    ) : ConfigureWalletEvent()

    data class PromptInputPassphrase(val func: (String) -> Unit) : ConfigureWalletEvent()
    data class ShowError(val message: String) : ConfigureWalletEvent()
}

data class ConfigureWalletState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val masterSignerMap: Map<String, SingleSigner> = emptyMap(),
    val selectedSigners: List<SignerModel> = emptyList(),
    val nonePassphraseSignerCount: Int = 0
)