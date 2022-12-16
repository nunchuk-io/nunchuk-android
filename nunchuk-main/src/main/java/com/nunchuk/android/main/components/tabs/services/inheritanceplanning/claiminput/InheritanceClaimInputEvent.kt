package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claiminput

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.MasterSigner

sealed class InheritanceClaimInputEvent {
    data class Loading(val isLoading: Boolean) : InheritanceClaimInputEvent()
    data class Error(val message: String) : InheritanceClaimInputEvent()
    object SubscriptionExpired : InheritanceClaimInputEvent()
    data class InActivated(val message: String) : InheritanceClaimInputEvent()
    data class ImportSuccess(val signer: SignerModel, val magicalPhrase: String) : InheritanceClaimInputEvent()
}

data class InheritanceClaimInputState(
//    val magicalPhrase: String = "paper cart report",
    val magicalPhrase: String = "",
    val backupPassword: String = "",
    val enableContinue: Boolean = false,
    val suggestions: List<String> = emptyList()
)