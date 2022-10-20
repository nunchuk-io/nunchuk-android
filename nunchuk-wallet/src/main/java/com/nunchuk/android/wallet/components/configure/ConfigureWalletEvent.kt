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

    data class PromptInputPassphrase(val signer: SignerModel) : ConfigureWalletEvent()
    data class ShowError(val message: String) : ConfigureWalletEvent()
    data class ShowRiskSignerDialog(val isShow: Boolean) : ConfigureWalletEvent()
    data class RequestCacheTapSignerXpub(val signer: SignerModel) : ConfigureWalletEvent()
    data class CacheTapSignerXpubError(val error: Throwable?) : ConfigureWalletEvent()
    data class NfcLoading(val isLoading: Boolean) : ConfigureWalletEvent()
}

data class ConfigureWalletState(
    val totalRequireSigns: Int = 0,
    val masterSigners: List<MasterSigner> = emptyList(),
    val remoteSigners: List<SingleSigner> = emptyList(),
    val masterSignerMap: Map<String, SingleSigner> = emptyMap(),
    val selectedSigners: Set<SignerModel> = emptySet(),
    val nonePassphraseSignerCount: Int = 0,
    val isShowPath: Boolean = false
)