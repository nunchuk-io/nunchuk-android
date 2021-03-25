package com.nunchuk.android.core.account

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import javax.inject.Inject

class AccountSharedPref @Inject constructor(
    context: Context,
    private val gson: Gson
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(ACCOUNT_PREFERENCE, Context.MODE_PRIVATE)

    fun getAccountInfo(): AccountInfo {
        val accountJson = sharedPreferences.getString(ACCOUNT_KEY, null)
        return if (accountJson.isNullOrEmpty()) {
            AccountInfo()
        } else {
            gson.fromJson(accountJson, AccountInfo::class.java)
        }
    }

    fun storeAccountInfo(accountInfo: AccountInfo) {
        sharedPreferences.edit().putString(ACCOUNT_KEY, gson.toJson(accountInfo)).apply()
    }

    fun clearAccountInfo() {
        sharedPreferences.edit().remove(ACCOUNT_KEY).apply()
    }

    companion object {
        private const val ACCOUNT_PREFERENCE = "ACCOUNT_PREFERENCE"
        private const val ACCOUNT_KEY = "ACCOUNT_KEY"
    }
}