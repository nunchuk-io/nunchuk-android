package com.nunchuk.android.core.account

import javax.inject.Inject
import javax.inject.Singleton

interface AccountManager {
    fun isAccountExisted(): Boolean

    fun isAccountActivated(): Boolean

    fun isStaySignedIn(): Boolean

    fun getAccount(): AccountInfo

    fun storeAccount(accountInfo: AccountInfo)

    fun signOut()
}

@Singleton
class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref
) : AccountManager {

    override fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    override fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    override fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    override fun getAccount() = accountSharedPref.getAccountInfo()

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override fun signOut() {
        accountSharedPref.clearAccountInfo()
    }

}