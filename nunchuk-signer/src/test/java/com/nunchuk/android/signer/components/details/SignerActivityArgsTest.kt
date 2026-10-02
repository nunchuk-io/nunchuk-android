package com.nunchuk.android.signer.components.details

import androidx.lifecycle.SavedStateHandle
import com.nunchuk.android.signer.components.details.message.SignMessageArgs
import com.nunchuk.android.type.SignerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignerActivityArgsTest {
    @Test
    fun `signer details reads existing intent keys from saved state`() {
        val args = SignerInfoArgs.fromSavedStateHandle(SavedStateHandle(mapOf(
            "id" to "master-id",
            "masterFingerprint" to "1234abcd",
            "name" to "My key",
            "customMessage" to "Key replaced",
            "derivationPath" to "m/84h/0h/0h",
            "justAdded" to true,
            "setPassphrase" to true,
            "isReplacePrimaryKey" to true,
            "is_master_signer" to true,
            "signer_type" to SignerType.SOFTWARE,
        )))

        assertEquals("master-id", args.id)
        assertEquals("1234abcd", args.masterFingerprint)
        assertEquals("My key", args.name)
        assertEquals("Key replaced", args.customMessage)
        assertEquals("m/84h/0h/0h", args.derivationPath)
        assertEquals(SignerType.SOFTWARE, args.signerType)
        assertTrue(args.justAdded)
        assertTrue(args.setPassphrase)
        assertTrue(args.isReplacePrimaryKey)
        assertTrue(args.isMasterSigner)
        assertNull(args.existingKey)
    }

    @Test
    fun `hardware signing retains fingerprint and path in saved state`() {
        val args = SignMessageArgs.fromSavedStateHandle(SavedStateHandle(mapOf(
            "master_signer_id" to "hardware-id",
            "signer_type" to SignerType.HARDWARE,
            "master_fingerprint" to "abcd1234",
            "derivation_path" to "m/84h/0h/0h",
        )))

        assertEquals("hardware-id", args.masterSignerId)
        assertEquals(SignerType.HARDWARE, args.signerType)
        assertEquals("abcd1234", args.masterFingerprint)
        assertEquals("m/84h/0h/0h", args.derivationPath)
    }

    @Test
    fun `software signing supports omitted hardware arguments`() {
        val args = SignMessageArgs.fromSavedStateHandle(SavedStateHandle(mapOf(
            "master_signer_id" to "software-id",
            "signer_type" to SignerType.SOFTWARE,
        )))

        assertEquals("", args.masterFingerprint)
        assertEquals("", args.derivationPath)
    }
}
