package com.nunchuk.android.signer.components.details

import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner

sealed class SignerInfoEvent {

    object Loading : SignerInfoEvent()

    data class UpdateNameSuccessEvent(val signerName: String) : SignerInfoEvent()

    data class UpdateNameErrorEvent(val message: String) : SignerInfoEvent()

    object RemoveSignerCompletedEvent : SignerInfoEvent()

    data class RemoveSignerErrorEvent(val message: String) : SignerInfoEvent()

    object HealthCheckSuccessEvent : SignerInfoEvent()

    data class HealthCheckErrorEvent(val message: String? = null, val e: Throwable? = null) : SignerInfoEvent()

    data class GetTapSignerBackupKeyEvent(val backupKeyPath: String) : SignerInfoEvent()

    data class GetTapSignerBackupKeyError(val e: Throwable?) : SignerInfoEvent()

    object TopUpXpubSuccess : SignerInfoEvent()

    data class TopUpXpubFailed(val e: Throwable?) : SignerInfoEvent()
}

data class SignerInfoState(val remoteSigner: SingleSigner? = null, val masterSigner: MasterSigner? = null, val nfcCardId: String? = null)