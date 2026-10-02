package com.nunchuk.android.signer.components.details

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.core.domain.membership.WalletsExistingKey
import com.nunchuk.android.type.SignerType

internal data class SignerInfoArgs(
    val id: String,
    val masterFingerprint: String,
    val name: String,
    val customMessage: String,
    val derivationPath: String,
    val justAdded: Boolean,
    val setPassphrase: Boolean,
    val isReplacePrimaryKey: Boolean,
    val isMasterSigner: Boolean,
    val signerType: SignerType,
    val existingKey: WalletsExistingKey? = null,
) {
    fun toBundle() = Bundle().apply {
        putString("id", id)
        putString("masterFingerprint", masterFingerprint)
        putString("name", name)
        putString("customMessage", customMessage)
        putString("derivationPath", derivationPath)
        putBoolean("justAdded", justAdded)
        putBoolean("setPassphrase", setPassphrase)
        putBoolean("isReplacePrimaryKey", isReplacePrimaryKey)
        putBoolean("is_master_signer", isMasterSigner)
        putSerializable("signer_type", signerType)
        putParcelable("existing_key", existingKey)
    }

    companion object {
        fun fromBundle(bundle: Bundle) = fromSavedStateHandle(SavedStateHandle(bundle.keySet().associateWith { bundle.get(it) }))

        fun fromSavedStateHandle(state: SavedStateHandle) = SignerInfoArgs(
            id = state.get<String>("id") ?: "",
            masterFingerprint = state.get<String>("masterFingerprint") ?: "",
            name = state.get<String>("name") ?: "",
            customMessage = state.get<String>("customMessage") ?: "",
            derivationPath = state.get<String>("derivationPath") ?: "",
            justAdded = state.get<Boolean>("justAdded") ?: false,
            setPassphrase = state.get<Boolean>("setPassphrase") ?: false,
            isReplacePrimaryKey = state.get<Boolean>("isReplacePrimaryKey") ?: false,
            isMasterSigner = state.get<Boolean>("is_master_signer") ?: false,
            signerType = requireNotNull(state.get<SignerType>("signer_type")),
            existingKey = state.get<WalletsExistingKey>("existing_key"),
        )
    }
}
