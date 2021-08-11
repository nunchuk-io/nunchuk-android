package com.nunchuk.android.app.splash

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.InitNunchukUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

internal class SplashViewModel @Inject constructor(
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    private fun initFlow() {
        val account = accountManager.getAccount()
        initNunchukUseCase.execute(account.email, account.chatId)
            .catch { event(InitErrorEvent(it.message.orUnknownError())) }
            .onEach { event(NavHomeScreenEvent) }
            .launchIn(viewModelScope)
    }

    fun handleNavigation() {
        when {
            !accountManager.isAccountExisted() -> event(NavSignInEvent)
            !accountManager.isAccountActivated() -> event(NavActivateAccountEvent)
            !accountManager.isStaySignedIn() || !accountManager.isLinkedWithMatrix() || !accountManager.isAccountExisted() -> event(NavSignInEvent)
            else -> initFlow()
        }
    }

}