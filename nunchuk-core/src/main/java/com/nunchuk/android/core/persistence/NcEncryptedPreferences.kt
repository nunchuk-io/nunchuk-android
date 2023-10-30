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
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nunchuk.android.core.account.AccountManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.matrix.android.sdk.api.auth.data.Credentials
import org.matrix.android.sdk.api.util.MatrixJsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NcEncryptedPreferences @Inject constructor(
    context: Context,
    private val accountManager: AccountManager
) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        APP_SHARE_PREFERENCE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun getUserId(): String {
        return accountManager.getAccount().chatId
    }

    private fun getWalletPinKey() = "$SP_WALLET_PIN-${getUserId()}"

    private fun getWalletPin(): String = prefs.getString(getWalletPinKey(), "").orEmpty()

    private val walletPinFlow: MutableStateFlow<String> = MutableStateFlow(getWalletPin())

    fun setWalletPin(value: String) {
        prefs.edit {
            putString(getWalletPinKey(), value)
            apply()
        }
        walletPinFlow.value = value
    }

    fun setMatrixCredential(credentials: Credentials) {
        prefs.edit {
            putString(credentials.userId, MatrixJsonParser.getMoshi().adapter(Credentials::class.java).toJson(credentials))
        }
    }

    fun getMatrixCredential(userId: String): Credentials? {
        val json = prefs.getString(userId, null)
        return if (json != null) {
            MatrixJsonParser.getMoshi().adapter(Credentials::class.java).fromJson(json)
        } else {
            null
        }
    }

    fun getWalletPinFlow(): Flow<String> {
        walletPinFlow.value = getWalletPin()
        return walletPinFlow
    }

    companion object {
        private const val PACKAGE_PREFIX = "com.nunchuk.android"
        private const val APP_SHARE_PREFERENCE_NAME = "${PACKAGE_PREFIX}.pref.encrypted"
        private const val SP_WALLET_PIN = "${PACKAGE_PREFIX}.key.wallet.pin"
    }

}