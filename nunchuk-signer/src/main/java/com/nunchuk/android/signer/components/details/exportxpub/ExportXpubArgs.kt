package com.nunchuk.android.signer.components.details.exportxpub

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.signer.components.details.exportxpub.exporter.XpubKey
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType

/**
 * A master signer is identified by [masterSignerId]; a stored key by [masterFingerprint] +
 * [derivationPath]. [signerType] and [signerTags] pick the key's exporter.
 */
internal data class ExportXpubArgs(
    val isMasterSigner: Boolean,
    val masterSignerId: String,
    val signerType: SignerType,
    val signerTags: List<SignerTag>,
    val masterFingerprint: String,
    val derivationPath: String,
    /** A stored key's spec when the caller already has it, so the export screen opens complete. */
    val descriptor: String = "",
) {
    val key get() = XpubKey(type = signerType, tags = signerTags, isMasterSigner = isMasterSigner)

    fun toBundle() = bundleOf(*toSavedState().toList().toTypedArray())

    /** The extras as the activity's [SavedStateHandle] sees them. */
    fun toSavedState(): Map<String, Any?> = mapOf(
        EXTRA_IS_MASTER_SIGNER to isMasterSigner,
        EXTRA_MASTER_SIGNER_ID to masterSignerId,
        EXTRA_SIGNER_TYPE to signerType.name,
        EXTRA_SIGNER_TAGS to ArrayList(signerTags.map { it.name }),
        EXTRA_MASTER_FINGERPRINT to masterFingerprint,
        EXTRA_DERIVATION_PATH to derivationPath,
        EXTRA_DESCRIPTOR to descriptor,
    )

    companion object {
        private const val EXTRA_IS_MASTER_SIGNER = "is_master_signer"
        private const val EXTRA_MASTER_SIGNER_ID = "master_signer_id"
        private const val EXTRA_SIGNER_TYPE = "signer_type"
        private const val EXTRA_SIGNER_TAGS = "signer_tags"
        private const val EXTRA_MASTER_FINGERPRINT = "master_fingerprint"
        private const val EXTRA_DERIVATION_PATH = "derivation_path"
        private const val EXTRA_DESCRIPTOR = "stored_descriptor"

        fun fromSavedStateHandle(state: SavedStateHandle) = ExportXpubArgs(
            isMasterSigner = state.get<Boolean>(EXTRA_IS_MASTER_SIGNER) ?: false,
            masterSignerId = state.get<String>(EXTRA_MASTER_SIGNER_ID).orEmpty(),
            signerType = SignerType.valueOf(requireNotNull(state.get<String>(EXTRA_SIGNER_TYPE))),
            signerTags = state.get<ArrayList<String>>(EXTRA_SIGNER_TAGS).orEmpty()
                .mapNotNull { name -> SignerTag.entries.find { it.name == name } },
            masterFingerprint = state.get<String>(EXTRA_MASTER_FINGERPRINT).orEmpty(),
            derivationPath = state.get<String>(EXTRA_DERIVATION_PATH).orEmpty(),
            descriptor = state.get<String>(EXTRA_DESCRIPTOR).orEmpty(),
        )
    }
}
