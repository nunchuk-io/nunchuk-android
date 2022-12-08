package com.nunchuk.android.main.membership.authentication

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.Transaction

sealed class WalletAuthenticationEvent {
    data class Loading(val isLoading: Boolean) : WalletAuthenticationEvent()
    data class ProcessFailure(val message: String) : WalletAuthenticationEvent()
    data class ShowError(val message: String) : WalletAuthenticationEvent()
    data class WalletAuthenticationSuccess(val signatures: Map<String, String>) :
        WalletAuthenticationEvent()

    class NfcLoading(val isLoading: Boolean, val isColdCard: Boolean = false) :
        WalletAuthenticationEvent()

    object ScanTapSigner : WalletAuthenticationEvent()
    object ScanColdCard : WalletAuthenticationEvent()
    object GenerateColdcardHealthMessagesSuccess : WalletAuthenticationEvent()
    object ShowAirgapOption : WalletAuthenticationEvent()
}

data class WalletAuthenticationState(
    val walletSigner: List<SignerModel> = emptyList(),
    val singleSigners: List<SingleSigner> = emptyList(),
    val signatures: Map<String, String> = emptyMap(),
    val transaction: Transaction? = null,
    val interactSingleSigner: SingleSigner? = null
)