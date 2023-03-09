package com.nunchuk.android.core.persistence

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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