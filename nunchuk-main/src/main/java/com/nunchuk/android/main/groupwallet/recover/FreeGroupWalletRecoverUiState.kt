package com.nunchuk.android.main.groupwallet.recover

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.ScriptNode
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner

data class FreeGroupWalletRecoverUiState(
    val signerUis: List<SignerModelRecoverUi> = emptyList(),
    val allSigners: List<SignerModel> = emptyList(),
    val isLoading: Boolean = false,
    val isFinishScreen: Boolean = false,
    val errorMessage: String = "",
    val wallet: Wallet? = null,
    val supportedTypes: List<SupportedSigner> = emptyList(),
    val event: FreeGroupWalletRecoverEvent = FreeGroupWalletRecoverEvent.None,
    val showAddKeyErrorDialog: Boolean = false,
    val scriptNode: ScriptNode? = null,
    val signerMap: Map<String, SignerModel?> = emptyMap(),
    val scriptNodeMuSig: ScriptNode? = null,
    val muSigSignerMap: Map<String, SignerModel?> = emptyMap(),
)

sealed class FreeGroupWalletRecoverEvent {
    data class RecoverSuccess(val walletName: String) : FreeGroupWalletRecoverEvent()
    data object None : FreeGroupWalletRecoverEvent()
}

data class SignerModelRecoverUi(
    val signer: SignerModel,
    val index: Int,
    val isInDevice: Boolean,
)