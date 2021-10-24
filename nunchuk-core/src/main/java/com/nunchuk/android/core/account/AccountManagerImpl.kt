package com.nunchuk.android.core.account

import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AccountManager {
    fun isAccountExisted(): Boolean

    fun isAccountActivated(): Boolean

    fun isStaySignedIn(): Boolean

    fun isLinkedWithMatrix(): Boolean

    fun getAccount(): AccountInfo

    fun storeAccount(accountInfo: AccountInfo)

    fun signOut()
}

@Singleton
internal class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref
) : AccountManager {

    override fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    override fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    override fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    override fun isLinkedWithMatrix() = SessionHolder.hasActiveSession() && accountSharedPref.getAccountInfo().chatId.isNotEmpty()

    override fun getAccount() = accountSharedPref.getAccountInfo()

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override fun signOut() {
        // TODO call Nunchuk SignOut Api
        accountSharedPref.clearAccountInfo()
        GlobalScope.launch {
            signOutMatrix().catch {
                Timber.e("signOut error ", it)
            }
        }
    }

    private fun signOutMatrix() = flow {
        emit(SessionHolder.clearActiveSession())
    }

}