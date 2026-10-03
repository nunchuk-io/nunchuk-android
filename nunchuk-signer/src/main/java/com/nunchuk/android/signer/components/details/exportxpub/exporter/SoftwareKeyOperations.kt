package com.nunchuk.android.signer.components.details.exportxpub.exporter

import com.nunchuk.android.usecase.GetMasterSignerUseCase
import com.nunchuk.android.usecase.GetSignerFromMasterSignerUseCase
import com.nunchuk.android.usecase.SendSignerPassphraseUseCase
import com.nunchuk.android.usecase.signer.ClearSignerPassphraseUseCase
import javax.inject.Inject

/** The SDK calls [SoftwareXpubExporter] makes on a software key. */
interface SoftwareKeyOperations {
    /** Whether the SDK still needs the key's passphrase before it can derive. */
    suspend fun needsPassphrase(masterSignerId: String): Result<Boolean>

    suspend fun sendPassphrase(masterSignerId: String, passphrase: String): Result<Unit>

    suspend fun deriveDescriptor(masterSignerId: String, path: String): Result<String>

    suspend fun clearPassphrase(masterSignerId: String): Result<Unit>
}

internal class SdkSoftwareKeyOperations @Inject constructor(
    private val getMasterSignerUseCase: GetMasterSignerUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val sendSignerPassphraseUseCase: SendSignerPassphraseUseCase,
    private val clearSignerPassphraseUseCase: ClearSignerPassphraseUseCase,
) : SoftwareKeyOperations {
    override suspend fun needsPassphrase(masterSignerId: String) =
        getMasterSignerUseCase(masterSignerId).map { it.device.needPassPhraseSent }

    override suspend fun sendPassphrase(masterSignerId: String, passphrase: String) =
        sendSignerPassphraseUseCase(SendSignerPassphraseUseCase.Param(signerId = masterSignerId, passphrase = passphrase))

    override suspend fun deriveDescriptor(masterSignerId: String, path: String) =
        getSignerFromMasterSignerUseCase(
            GetSignerFromMasterSignerUseCase.Params(masterSignerId = masterSignerId, newPath = path)
        ).map { it.descriptor }

    override suspend fun clearPassphrase(masterSignerId: String) = clearSignerPassphraseUseCase(masterSignerId)
}
