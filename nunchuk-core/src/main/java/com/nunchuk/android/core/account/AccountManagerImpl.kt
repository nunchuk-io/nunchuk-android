package com.nunchuk.android.core.account

import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.AppUpdateStateHolder
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
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

    fun signOut(onStartSignOut: () -> Unit = {}, onSignedOut: () -> Unit = {})

    fun clearUserData()
}

@Singleton
internal class AccountManagerImpl @Inject constructor(
    private val accountSharedPref: AccountSharedPref,
) : AccountManager {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun isAccountExisted() = accountSharedPref.getAccountInfo().token.isNotBlank()

    override fun isAccountActivated() = accountSharedPref.getAccountInfo().activated

    override fun isStaySignedIn() = accountSharedPref.getAccountInfo().staySignedIn

    override fun isLinkedWithMatrix() = SessionHolder.hasActiveSession() && accountSharedPref.getAccountInfo().chatId.isNotEmpty()

    override fun getAccount() = accountSharedPref.getAccountInfo()

    override fun storeAccount(accountInfo: AccountInfo) {
        accountSharedPref.storeAccountInfo(accountInfo)
    }

    override fun signOut(onStartSignOut: () -> Unit, onSignedOut: () -> Unit) {
        // TODO call Nunchuk SignOut Api
        scope.launch {
            signOutMatrix()
                .flowOn(Dispatchers.IO)
                .onStart { onStartSignOut() }
                .onException { Timber.e("signOut error ", it) }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    onSignedOut()
                }
                .collect {
                    clearUserData()
                }
        }
    }

    override fun clearUserData() {
        AppUpdateStateHolder.reset()
        accountSharedPref.clearAccountInfo()
        SessionHolder.activeSession = null
        SessionHolder.currentRoom = null
    }

    private fun signOutMatrix() = flow {
        emit(SessionHolder.clearActiveSession())
    }

}