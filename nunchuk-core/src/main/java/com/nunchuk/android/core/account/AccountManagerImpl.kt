package com.nunchuk.android.core.account

import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.util.AppUpdateStateHolder
import javax.inject.Inject
import javax.inject.Singleton

interface AccountManager {
    fun isHasAccountBefore(): Boolean

    fun isAccountExisted(): Boolean

    fun isAccountActivated(): Boolean

    fun isStaySignedIn(): Boolean

    fun getAccount(): AccountInfo

    fun getPrimaryKeyInfo(): PrimaryKeyInfo?

    fun storeAccount(accountInfo: AccountInfo)

    fun storePrimaryKeyInfo(primaryKeyInfo: PrimaryKeyInfo)

    suspend fun signOut()

    fun clearUserData()

    fun isFreshInstall(): Boolean

    fun clearFreshInstall()

    fun loginType(): Int
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

    override fun getPrimaryKeyInfo(): PrimaryKeyInfo? {
        val account = getAccount()
        if (account.loginType != SignInMode.PRIMARY_KEY.value) return null
        return account.primaryKeyInfo
    }

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override fun storePrimaryKeyInfo(primaryKeyInfo: PrimaryKeyInfo) {
        val account = accountSharedPref.getAccountInfo()
        storeAccount(account.copy(primaryKeyInfo = primaryKeyInfo))
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

    override fun loginType(): Int = accountSharedPref.getAccountInfo().loginType
}