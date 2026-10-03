package com.nunchuk.android.signer.components.details.exportxpub.exporter

import android.nfc.tech.IsoDep
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import javax.inject.Inject

/**
 * How one kind of key produces the `[xfp/path]xpub` string for Key info → Export XPUB.
 *
 * Every key-type decision of the flow lives in an implementation: whether the key offers the
 * option at all ([supports]), whether the user first picks an account ([selectsAccount]), and
 * what the key needs from the user or a device before it can answer ([XpubStep.NeedInput]). The
 * view model and the screens stay type-agnostic, so a new key type is one more implementation
 * bound in [ExportXpubModule] — plus an [XpubAction] handler in `ExportXpubActivity` only if it
 * needs a kind of input none of the existing ones covers.
 */
interface XpubExporter {
    fun supports(key: XpubKey): Boolean

    /** True when the user picks an account or a custom path; false exports the key's own spec. */
    val selectsAccount: Boolean

    /** [XpubRequest.path] is null when [selectsAccount] is false. */
    suspend fun export(request: XpubRequest): XpubStep

    /** Called with what the user or the device supplied after an [XpubStep.NeedInput]. */
    suspend fun resume(request: XpubRequest, input: XpubInput): XpubStep
}

/** What Key info knows about the key, enough to pick its [XpubExporter]. */
data class XpubKey(
    val type: SignerType,
    val tags: List<SignerTag>,
    val isMasterSigner: Boolean,
)

data class XpubRequest(
    val masterSignerId: String,
    val masterFingerprint: String,
    val derivationPath: String,
    val path: String?,
)

sealed interface XpubStep {
    data class Done(val descriptor: String) : XpubStep
    data class NeedInput(val action: XpubAction) : XpubStep
    data class Failed(val error: Throwable) : XpubStep
}

/** Input the flow has to collect before [XpubExporter.resume]. */
sealed interface XpubAction {
    data object Passphrase : XpubAction

    /** The xpub for this path isn't cached on the phone: the card has to be tapped to read it. */
    data object NfcTap : XpubAction
}

sealed interface XpubInput {
    data class Passphrase(val value: String) : XpubInput
    class Nfc(val isoDep: IsoDep, val cvc: String) : XpubInput
}

fun Result<String>.toXpubStep(): XpubStep = fold(
    onSuccess = { XpubStep.Done(it) },
    onFailure = { XpubStep.Failed(it) },
)

class XpubExporters @Inject constructor(
    private val exporters: Set<@JvmSuppressWildcards XpubExporter>,
) {
    fun find(key: XpubKey): XpubExporter? = exporters.firstOrNull { it.supports(key) }

    fun canExport(key: XpubKey) = find(key) != null
}
