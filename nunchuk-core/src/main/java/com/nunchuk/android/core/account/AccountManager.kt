package com.nunchuk.android.core.account

import javax.inject.Inject

class AccountManager @Inject constructor(
    private val accountSharedPref: AccountSharedPref
) {

    fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    fun getAccount() = accountSharedPref.getAccountInfo()

    fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

}