package com.nunchuk.android.app.splash

import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.process
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.usecase.GetAppSettingsUseCase
import com.nunchuk.android.usecase.InitNunchukUseCase
import javax.inject.Inject

internal class SplashViewModel @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingsUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    fun initFlow() {
        process(getAppSettingUseCase::execute, ::initNunchuk) {
            event(InitErrorEvent(it.messageOrUnknownError()))
        }
    }

    private fun initNunchuk(appSettings: AppSettings) {
        process({
            initNunchukUseCase.execute(appSettings = appSettings)
        }, {
            handleNavigation()
        }, {
            event(InitErrorEvent(it.messageOrUnknownError()))
        })
    }

    private fun handleNavigation() {
        when {
            !accountManager.isAccountExisted() -> event(NavSignInEvent)
            !accountManager.isAccountActivated() -> event(NavActivateAccountEvent)
            !accountManager.isStaySignedIn() || !accountManager.isLinkedWithMatrix() || !accountManager.isAccountExisted()-> event(NavSignInEvent)
            else -> event(NavHomeScreenEvent)
        }
    }

}