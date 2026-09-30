package com.nunchuk.android.signer

import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.core.signer.SignerModel
import org.junit.Assert.*
import org.junit.Test

class SignerIntroSelectionStateTest {
    @Test fun `restore clears pending selection when eligible keys were removed or filtered out`() {
        val handle = SavedStateHandle()
        SignerIntroSelectionState(handle).save(KeyType.JADE)
        val restored = SignerIntroSelectionState(handle)
        assertNull(restored.restore { key ->
            assertEquals(KeyType.JADE, key)
            emptyList()
        })
        assertNull(restored.restore { error("Cleared selection must not be retried") })
    }

    @Test fun `restore reopens only the saved sheet using freshly filtered keys`() {
        val handle = SavedStateHandle()
        SignerIntroSelectionState(handle).save(KeyType.KEYSTONE)
        val key = SignerModel(
            id = "key", name = "Key", derivationPath = "m/48h/0h/0h/2h",
            fingerPrint = "1234abcd", isMasterSigner = true,
        )
        val selection = SignerIntroSelectionState(handle).restore { type ->
            assertEquals(KeyType.KEYSTONE, type)
            listOf(key)
        }
        assertEquals(ExistingSignerSelection(KeyType.KEYSTONE, listOf(key)), selection)
    }

    @Test fun `dismissed selection stays dismissed after recreation`() {
        val handle = SavedStateHandle()
        val state = SignerIntroSelectionState(handle)
        state.save(KeyType.TAPSIGNER)
        state.clear()
        assertNull(SignerIntroSelectionState(handle).restore { error("No pending selection") })
    }

    @Test fun `unknown saved key type is cleared without throwing or choosing a device`() {
        val handle = SavedStateHandle(mapOf("signer_intro_pending_key" to "REMOVED_DEVICE"))
        val state = SignerIntroSelectionState(handle)
        assertNull(state.restore { error("Unknown key must not be resolved") })
        assertFalse(handle.contains("signer_intro_pending_key"))
    }
}
