package com.nunchuk.android.signer.components.details.exportxpub.exporter

import com.nunchuk.android.type.SignerType
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A software key derives locally. One with a passphrase gets it for this export only: it is
 * cleared from the SDK right after, as signing does — even when the export is canceled midway.
 */
internal class SoftwareXpubExporter @Inject constructor(
    private val operations: SoftwareKeyOperations,
) : XpubExporter {
    override fun supports(key: XpubKey) = key.isMasterSigner && key.type == SignerType.SOFTWARE

    override val selectsAccount = true

    /** Asks the SDK each time: whether the passphrase is still needed is only current there. */
    override suspend fun export(request: XpubRequest): XpubStep {
        val needsPassphrase = operations.needsPassphrase(request.masterSignerId)
            .getOrElse { return XpubStep.Failed(it) }
        return if (needsPassphrase) XpubStep.NeedInput(XpubAction.Passphrase) else derive(request)
    }

    override suspend fun resume(request: XpubRequest, input: XpubInput): XpubStep {
        if (input !is XpubInput.Passphrase) return export(request)
        // Sending sits inside the try too: the SDK may hold the passphrase even if the call that
        // sent it was canceled before returning.
        return try {
            operations.sendPassphrase(request.masterSignerId, input.value)
                .onFailure { return XpubStep.Failed(it) }
            derive(request)
        } finally {
            withContext(NonCancellable) { operations.clearPassphrase(request.masterSignerId) }
        }
    }

    private suspend fun derive(request: XpubRequest): XpubStep =
        operations.deriveDescriptor(request.masterSignerId, request.path.orEmpty()).toXpubStep()
}
