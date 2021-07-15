package com.nunchuk.android.main.components.tabs.account

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.provider.AppInfoProvider
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val appInfoProvider: AppInfoProvider
) : NunchukViewModel<AccountState, AccountEvent>() {

    override val initialState = AccountState()

    init {
        updateState {
            copy(
                email = accountManager.getAccount().email,
                appVersion = appInfoProvider.getAppVersion()
            )
        }
    }

    fun handleSignOutEvent() {
        accountManager.signOut()
        signOutMatrix()
        event(AccountEvent.SignOutEvent)
    }

    private fun signOutMatrix() {
        viewModelScope.launch {
            SessionHolder.currentSession?.signOut(true)
        }
    }

}