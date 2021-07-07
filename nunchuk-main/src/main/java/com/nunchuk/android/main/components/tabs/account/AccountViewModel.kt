package com.nunchuk.android.main.components.tabs.account

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, AccountEvent>() {

    override val initialState = Unit

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