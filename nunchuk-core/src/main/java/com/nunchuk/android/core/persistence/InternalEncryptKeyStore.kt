/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.core.persistence

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import timber.log.Timber
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the per-device `internal_encrypt_key` that libnunchuk uses to encrypt software-signer
 * secrets (mnemonic, master xprv) in its database.
 *
 * libnunchuk needs the raw 32 bytes and the AndroidKeyStore will not export a raw AES key, so the
 * bytes are generated here with [SecureRandom] and wrapped (AES-256-GCM) by a non-exportable
 * Keystore key. Only `version | ivLen | iv | ciphertext` is written, to [Context.getNoBackupFilesDir],
 * which Auto Backup and device transfer never copy (the manifest also disables backup outright).
 * The key is never synced: a copy of the app data on another device must not be able to read the
 * mnemonics.
 *
 * Losing this key loses the on-device mnemonics. The only time that is accepted is when the key is
 * provably gone (Keystore entry missing or invalidated, wrapped blob missing or failing its GCM tag);
 * any other failure is thrown so the caller fails closed instead of minting a new key and wiping
 * data that a retry could still have read.
 */
@Singleton
class InternalEncryptKeyStore @Inject constructor(
    private val context: Context,
) {
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    private val keyFile: File get() = File(context.noBackupFilesDir, KEY_FILE_NAME)
    private val recoveryFile: File get() = File(context.noBackupFilesDir, RECOVERY_FILE_NAME)

    /**
     * Returns the raw key. The caller owns the array and should zero it once libnunchuk has it.
     * Throws if the key exists but cannot be read right now.
     */
    @Synchronized
    fun getOrCreateKey(): ByteArray {
        val wrappingKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        val blob = keyFile.takeIf { it.exists() }?.readBytes()

        if (wrappingKey != null && blob != null) {
            try {
                return unwrap(wrappingKey, blob)
            } catch (e: Exception) {
                if (!e.isPermanentKeyLoss()) throw e
                Timber.e(e, "Internal encrypt key is unrecoverable")
            }
        }
        // Only one half left (or an unreadable pair): a key existed and is gone for good.
        // Neither half: first launch on this install, nothing was ever encrypted.
        val lost = wrappingKey != null || blob != null
        return createKey(markRecoveryPending = lost)
    }

    /**
     * True when a previous key was lost and the database identified by [identity] has not been
     * swept yet: open it in recovery mode so unreadable ciphertext is erased instead of thrown.
     */
    @Synchronized
    fun isRecoveryPending(identity: String): Boolean =
        recoveryFile.exists() && identity !in recoveryFile.readLines()

    @Synchronized
    fun markRecovered(identity: String) {
        if (!recoveryFile.exists()) return
        if (identity in recoveryFile.readLines()) return
        recoveryFile.appendText(identity + "\n")
    }

    private fun createKey(markRecoveryPending: Boolean): ByteArray {
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
        val raw = ByteArray(KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val blob = wrap(generateWrappingKey(), raw)
        // Mark before the new key is persisted: if we die in between, the next launch sees a
        // lost key again and keeps the recovery marker rather than silently dropping it.
        if (markRecoveryPending) writeAtomically(recoveryFile, ByteArray(0))
        writeAtomically(keyFile, blob)
        return raw
    }

    private fun generateWrappingKey(): SecretKey {
        fun spec(strongBox: Boolean) = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            // No setUserAuthenticationRequired: it invalidates the key on biometric changes,
            // which would take every mnemonic with it.
            .apply { if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) setIsStrongBoxBacked(true) }
            .build()

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        if (hasStrongBox()) {
            try {
                generator.init(spec(strongBox = true))
                return generator.generateKey()
            } catch (e: StrongBoxUnavailableException) {
                Timber.w(e, "StrongBox unavailable, falling back to TEE")
            }
        }
        generator.init(spec(strongBox = false))
        return generator.generateKey()
    }

    private fun hasStrongBox(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    private fun wrap(key: SecretKey, raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key) }
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(raw)
        return byteArrayOf(BLOB_VERSION, iv.size.toByte()) + iv + ciphertext
    }

    private fun unwrap(key: SecretKey, blob: ByteArray): ByteArray {
        if (blob.size < 2 || blob[0] != BLOB_VERSION) throw AEADBadTagException("Unknown key blob")
        val ivLen = blob[1].toInt()
        if (blob.size <= 2 + ivLen) throw AEADBadTagException("Truncated key blob")
        val iv = blob.copyOfRange(2, 2 + ivLen)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        val raw = cipher.doFinal(blob, 2 + ivLen, blob.size - 2 - ivLen)
        if (raw.size != KEY_SIZE_BYTES) {
            raw.fill(0)
            throw AEADBadTagException("Unexpected key length")
        }
        return raw
    }

    private fun writeAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.outputStream().use { out ->
            out.write(bytes)
            out.fd.sync()
        }
        check(tmp.renameTo(target)) { "Failed to persist ${target.name}" }
    }

    private fun Exception.isPermanentKeyLoss() =
        this is AEADBadTagException || this is KeyPermanentlyInvalidatedException

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.nunchuk.android.key.internal_encrypt"
        private const val KEY_FILE_NAME = "internal_encrypt_key.bin"
        private const val RECOVERY_FILE_NAME = "internal_encrypt_key.recovery"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
        private const val KEY_SIZE_BYTES = 32
        private const val BLOB_VERSION: Byte = 1
    }
}
