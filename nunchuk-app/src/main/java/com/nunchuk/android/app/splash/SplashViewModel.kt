package com.nunchuk.android.app.splash

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManagerImpl
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.GetAppSettingsUseCase
import com.nunchuk.android.usecase.InitNunchukUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class SplashViewModel @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingsUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManagerImpl
) : NunchukViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    fun initFlow() {
        viewModelScope.launch {
            when (val settingsResult = getAppSettingUseCase.execute()) {
                is Result.Success -> {
                    initNunchuk(settingsResult.data)
                }
                is Result.Error -> event(InitErrorEvent(settingsResult.exception.message))
            }
        }
    }

    private fun initNunchuk(appSettings: AppSettings) {
        viewModelScope.launch {
            when (val result = initNunchukUseCase.execute(appSettings = appSettings)) {
                is Result.Success -> {
                    handleNavigation()
                }
                is Result.Error -> event(InitErrorEvent(result.exception.message))
            }
        }
    }

    private fun handleNavigation() {
        when {
            !accountManager.isAccountExisted() -> event(NavCreateAccountEvent)
            !accountManager.isAccountActivated() -> event(NavActivateAccountEvent)
            !accountManager.isStaySignedIn() -> event(NavSignInEvent)
            else -> event(NavHomeScreenEvent)
        }
    }

}