package com.nunchuk.android.core.persistence

import android.content.Context
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NcEncryptedPreferences @Inject constructor(context: Context) {

    private val prefs = EncryptedSharedPreferences.create(
        context,
        APP_SHARE_PREFERENCE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val walletPinFlow: MutableStateFlow<String> = MutableStateFlow(getWalletPin())

    private fun getWalletPin(): String = prefs.getString(SP_WALLET_PIN, "").orEmpty()

    fun setWalletPin(value: String) {
        prefs.edit {
            putString(SP_WALLET_PIN, value)
            apply()
        }
        walletPinFlow.value = value
    }

    fun getWalletPinFlow(): Flow<String> {
        return walletPinFlow
    }

    fun clear() {
        walletPinFlow.value = ""
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PACKAGE_PREFIX = "com.nunchuk.android"
        private const val APP_SHARE_PREFERENCE_NAME = "${PACKAGE_PREFIX}.pref.encrypted"
        private const val SP_WALLET_PIN = "${PACKAGE_PREFIX}.key.wallet.pin"
    }

}