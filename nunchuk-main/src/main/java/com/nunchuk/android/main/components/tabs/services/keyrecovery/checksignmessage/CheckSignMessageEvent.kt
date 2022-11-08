package com.nunchuk.android.main.components.tabs.services.keyrecovery.checksignmessage

import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.SingleSigner

sealed class CheckSignMessageEvent {
    data class Loading(val isLoading: Boolean) : CheckSignMessageEvent()
    data class ProcessFailure(val message: String) : CheckSignMessageEvent()
    data class GetSignersSuccess(val signers: List<SignerModel>) : CheckSignMessageEvent()
    data class CheckSignMessageSuccess(val signatures: HashMap<String, String>) :
        CheckSignMessageEvent()

    object OpenScanDataTapsigner : CheckSignMessageEvent()
    object ContinueClick : CheckSignMessageEvent()
}

data class CheckSignMessageState(
    val signerModels: List<SignerModel> = emptyList(),
    val singleSigners: List<SingleSigner> = emptyList(),
    val signatures: HashMap<String, String> = HashMap(),
    val interactSingleSigner: SingleSigner? = null
)