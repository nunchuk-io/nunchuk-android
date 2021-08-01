package com.nunchuk.android.signer.components.details

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class SignerInfoEvent {

    data class UpdateNameSuccessEvent(val signerName: String) : SignerInfoEvent()

    data class UpdateNameErrorEvent(val message: String) : SignerInfoEvent()

    object RemoveSignerCompletedEvent : SignerInfoEvent()

    data class RemoveSignerErrorEvent(val message: String) : SignerInfoEvent()
}

data class SignerInfoState(val remoteSigner: SingleSigner? = null, val masterSigner: MasterSigner? = null)