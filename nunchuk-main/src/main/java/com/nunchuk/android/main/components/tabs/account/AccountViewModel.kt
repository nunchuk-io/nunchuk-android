package com.nunchuk.android.main.components.tabs.account

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, AccountEvent>() {

    override val initialState = Unit

    fun handleSignOutEvent() {
        accountManager.signOut()
        event(AccountEvent.SignOutEvent)
    }

}