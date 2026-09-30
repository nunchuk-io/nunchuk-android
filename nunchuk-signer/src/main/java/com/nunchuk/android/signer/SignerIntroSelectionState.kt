package com.nunchuk.android.signer

import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.core.signer.SignerModel

data class ExistingSignerSelection(val keyType: KeyType, val signers: List<SignerModel>)

/** Restores only the reuse sheet. This boundary cannot dispatch navigation. */
internal class SignerIntroSelectionState(private val savedStateHandle: SavedStateHandle) {
    fun save(keyType: KeyType) {
        savedStateHandle[PENDING_KEY] = keyType.name
    }

    fun clear() {
        savedStateHandle.remove<String>(PENDING_KEY)
    }

    fun restore(findSigners: (KeyType) -> List<SignerModel>): ExistingSignerSelection? {
        val name = savedStateHandle.get<String>(PENDING_KEY) ?: return null
        val keyType = KeyType.entries.firstOrNull { it.name == name }
        val signers = keyType?.let(findSigners).orEmpty()
        if (keyType == null || signers.isEmpty()) {
            clear()
            return null
        }
        return ExistingSignerSelection(keyType, signers)
    }

    private companion object {
        const val PENDING_KEY = "signer_intro_pending_key"
    }
}
