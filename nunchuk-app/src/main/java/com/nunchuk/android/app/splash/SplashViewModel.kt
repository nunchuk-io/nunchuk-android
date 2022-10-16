package com.nunchuk.android.app.splash

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.splash.SplashEvent.InitErrorEvent
import com.nunchuk.android.app.splash.SplashEvent.NavActivateAccountEvent
import com.nunchuk.android.app.splash.SplashEvent.NavHomeScreenEvent
import com.nunchuk.android.app.splash.SplashEvent.NavIntroEvent
import com.nunchuk.android.app.splash.SplashEvent.NavSignInEvent
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.core.matrix.MatrixInitializerUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager,
    private val matrixInitializerUseCase: MatrixInitializerUseCase,
    private val signInModeHolder: SignInModeHolder
) : NunchukViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    init {
        initSignInMode()
    }

    private fun initSignInMode() {
        val isAccountExist = accountManager.isAccountExisted()
        val loginType = accountManager.loginType()
        if (isAccountExist) {
            when (loginType) {
                SignInMode.UNKNOWN.value, SignInMode.EMAIL.value -> {
                    signInModeHolder.setCurrentMode(SignInMode.EMAIL)
                }
                SignInMode.PRIMARY_KEY.value -> {
                    signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
                }
                else -> {
                    signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
                }
            }
        } else {
            signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
        }
        if (isAccountExist) {
            accountManager.clearFreshInstall()
        }
    }

    fun initFlow() {
        val account = accountManager.getAccount()
        viewModelScope.launch {
            matrixInitializerUseCase(Unit)
            val accountId = if (signInModeHolder.getCurrentMode().isPrimaryKey()) {
                account.username
            } else {
                account.email
            }
            initNunchukUseCase.execute(accountId = accountId)
                .flowOn(Dispatchers.IO)
                .onException { event(InitErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    when {
                        accountManager.isFreshInstall() -> {
                            event(NavIntroEvent)
                            accountManager.clearFreshInstall()
                        }
                        accountManager.isAccountExisted() && !accountManager.isAccountActivated() -> event(
                            NavActivateAccountEvent
                        )
                        accountManager.isHasAccountBefore() && !accountManager.isStaySignedIn() -> event(
                            NavSignInEvent
                        )
                        else -> event(NavHomeScreenEvent(account.token, account.deviceId))
                    }
                }
        }
    }

}