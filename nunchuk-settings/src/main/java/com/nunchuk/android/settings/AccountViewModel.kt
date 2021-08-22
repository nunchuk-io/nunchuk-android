package com.nunchuk.android.settings

import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.provider.AppInfoProvider
import javax.inject.Inject

internal class AccountViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val appInfoProvider: AppInfoProvider
) : NunchukViewModel<AccountState, AccountEvent>() {

    override val initialState = AccountState()

    init {
        updateState {
            copy(
                account = accountManager.getAccount(),
                appVersion = appInfoProvider.getAppVersion()
            )
        }
    }

    fun handleSignOutEvent() {
        accountManager.signOut()
        event(AccountEvent.SignOutEvent)
    }

}