package com.nunchuk.android.signer.components.details.message

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.type.SignerType

internal data class SignMessageArgs(
    val masterSignerId: String,
    val signerType: SignerType,
    val masterFingerprint: String,
    val derivationPath: String,
) {
    fun toBundle() = Bundle().apply {
        putString("master_signer_id", masterSignerId)
        putSerializable("signer_type", signerType)
        putString("master_fingerprint", masterFingerprint)
        putString("derivation_path", derivationPath)
    }

    companion object {
        fun fromBundle(bundle: Bundle) = fromSavedStateHandle(SavedStateHandle(bundle.keySet().associateWith { bundle.get(it) }))

        fun fromSavedStateHandle(state: SavedStateHandle) = SignMessageArgs(
            masterSignerId = state.get<String>("master_signer_id") ?: "",
            signerType = requireNotNull(state.get<SignerType>("signer_type")),
            masterFingerprint = state.get<String>("master_fingerprint") ?: "",
            derivationPath = state.get<String>("derivation_path") ?: "",
        )
    }
}
