package com.nunchuk.android.signer.components.details.exportxpub.exporter

import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import javax.inject.Inject

/** A key held as a single signer exports the key spec it already has, path as the lib returns it. */
internal class StoredKeyXpubExporter @Inject constructor(
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
) : XpubExporter {
    override fun supports(key: XpubKey) = !key.isMasterSigner

    override val selectsAccount = false

    override suspend fun export(request: XpubRequest): XpubStep =
        getRemoteSignerUseCase(
            GetRemoteSignerUseCase.Data(request.masterFingerprint, request.derivationPath)
        ).map { it.descriptor }.toXpubStep()

    override suspend fun resume(request: XpubRequest, input: XpubInput) = export(request)
}
