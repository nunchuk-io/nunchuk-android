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

package com.nunchuk.android.core.biometric

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps the biometric-login config with an AES-256-GCM key from the AndroidKeyStore. The config
 * carries the BIP39 mnemonic that signs the server login challenge, so a cleartext copy in the
 * DataStore file would be a silent account takeover for anyone who can read the app sandbox.
 *
 * Gap: the key is not `setUserAuthenticationRequired(true)`, because the enable-biometric flow
 * writes the config where no prompt is shown. Binding the prompt to a CryptoObject over this key
 * is the stronger fix and is tracked separately.
 */
@Singleton
class BiometricSecretEncryption @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    /** False for values written by older builds, which were cleartext. */
    fun isEncrypted(payload: String): Boolean = payload.startsWith(PREFIX)

    /** Falls back to returning [plainText] unwrapped if the keystore is unusable. */
    fun encrypt(plainText: String): String = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        }
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        "$PREFIX${cipher.iv.toHex()}$SEPARATOR${cipherText.toHex()}"
    }.getOrElse {
        Timber.e(it, "Failed to encrypt biometric config")
        plainText
    }

    /**
     * Null when the payload can't be decrypted (key deleted, invalidated, or restored onto another
     * device). Callers should then treat biometric login as not set up.
     */
    fun decrypt(payload: String): String? {
        if (!isEncrypted(payload)) return null
        return runCatching {
            val parts = payload.removePrefix(PREFIX).split(SEPARATOR)
            require(parts.size == 2) { "Malformed biometric payload" }
            val iv = parts[0].hexToBytes()
            val cipherText = parts[1].hexToBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.onFailure {
            Timber.e(it, "Failed to decrypt biometric config")
        }.getOrNull()
    }

    /** Renders any stored payload permanently unreadable. */
    fun deleteKey() {
        runCatching { keyStore.deleteEntry(KEY_ALIAS) }
            .onFailure { Timber.e(it, "Failed to delete biometric key") }
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )
        return generator.generateKey()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.hexToBytes(): ByteArray {
        require(length % 2 == 0) { "Malformed hex payload" }
        return ByteArray(length / 2) { substring(it * 2, it * 2 + 2).toInt(16).toByte() }
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "com.nunchuk.android.key.biometric.config"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_LENGTH_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val PREFIX = "enc1:"
        private const val SEPARATOR = ":"
    }
}
