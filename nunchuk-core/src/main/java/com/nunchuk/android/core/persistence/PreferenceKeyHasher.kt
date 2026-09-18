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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Derives opaque preference key names from secret values. Key names are stored verbatim in the
 * DataStore file, so interpolating a secret (`decoy_pin_123456`) hands it to anyone who can read
 * the sandbox. A short numeric PIN is too small for a bare digest — the whole space enumerates
 * offline — so this HMACs under an AndroidKeyStore key that can't be pulled from a disk image.
 */
@Singleton
class PreferenceKeyHasher @Inject constructor() {

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }

    /** Stable, opaque token for [value], safe to embed in a preference key name. */
    fun derive(value: String): String = runCatching {
        val mac = Mac.getInstance(ALGORITHM).apply { init(getOrCreateKey()) }
        mac.doFinal(value.toByteArray(Charsets.UTF_8)).toHex()
    }.getOrElse {
        Timber.e(it, "Failed to derive preference key")
        // Degraded, but still not the raw secret. Only reachable if the keystore is unusable.
        value.hashCode().toUInt().toString(16)
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(ALGORITHM, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build()
        )
        return generator.generateKey()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_HMAC_SHA256
        private const val KEY_ALIAS = "com.nunchuk.android.key.pref.name"
    }
}
