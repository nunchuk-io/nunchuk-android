package com.nunchuk.android.app.splash

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    init {
        val isAccountExist = accountManager.isAccountExisted()
        SignInModeHolder.currentMode = if (isAccountExist) SignInMode.NORMAL else SignInMode.GUEST_MODE
        if (isAccountExist) {
            accountManager.clearFreshInstall()
        }
    }

    private fun initFlow() {
        val account = accountManager.getAccount()
        viewModelScope.launch {
            initNunchukUseCase.execute(accountId = account.email)
                .flowOn(Dispatchers.IO)
                .onException { event(InitErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { event(NavHomeScreenEvent) }
        }
    }

    fun handleNavigation() {
        when {
            accountManager.isFreshInstall() -> {
                event(NavIntroEvent)
                accountManager.clearFreshInstall()
            }
            accountManager.isAccountExisted() && !accountManager.isAccountActivated() -> event(NavActivateAccountEvent)
            accountManager.isHasAccountBefore() && !accountManager.isStaySignedIn() -> event(NavSignInEvent)
            else -> initFlow()
        }
    }

}