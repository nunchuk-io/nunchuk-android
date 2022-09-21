package com.nunchuk.android.core.account

import com.nunchuk.android.core.util.AppUpdateStateHolder
import javax.inject.Inject
import javax.inject.Singleton

interface AccountManager {
    fun isHasAccountBefore(): Boolean

    fun isAccountExisted(): Boolean

    fun isAccountActivated(): Boolean

    fun isStaySignedIn(): Boolean

    fun getAccount(): AccountInfo

    fun storeAccount(accountInfo: AccountInfo)

    suspend fun signOut()

    fun clearUserData()

    fun isFreshInstall(): Boolean

    fun clearFreshInstall()
}

@Singleton
internal class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref,
) : AccountManager {

    override fun isHasAccountBefore(): Boolean = accountSharedPref.isHasAccountBefore()

    override fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    override fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    override fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    override fun getAccount() = accountSharedPref.getAccountInfo()

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override suspend fun signOut() {
        clearUserData()
    }

    override fun clearUserData() {
        AppUpdateStateHolder.reset()
        accountSharedPref.clearAccountInfo()
    }

    override fun isFreshInstall(): Boolean {
        return accountSharedPref.isFreshInstall()
    }

    override fun clearFreshInstall() {
        accountSharedPref.clearFreshInstall()
    }
}