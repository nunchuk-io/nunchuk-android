package com.nunchuk.android.app.splash

import android.util.Log
import com.nunchuk.android.app.splash.SplashEvent.*
import com.nunchuk.android.arch.ext.defaultSchedulers
import com.nunchuk.android.arch.vm.NCViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.usecase.CreateSignerUseCase
import com.nunchuk.android.usecase.GetAppSettingsUseCase
import com.nunchuk.android.usecase.GetRemoteSignerUseCase
import com.nunchuk.android.usecase.InitNunchukUseCase
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SplashViewModel @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingsUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val createSignerUseCase: CreateSignerUseCase,
    private val getRemoteSignerUseCase: GetRemoteSignerUseCase,
    private val accountManager: AccountManager
) : NCViewModel<Unit, SplashEvent>() {

    override val initialState = Unit

    fun initFlow() {
        getAppSettingUseCase.execute()
            .flatMapCompletable(initNunchukUseCase::execute)
            .delay(2, TimeUnit.SECONDS)
            .defaultSchedulers()
            .doAfterTerminate(::createRemoteSigner)
            .subscribe(::handleNavigation) {
                event(InitErrorEvent(it.message))
            }
            .addToDisposables()
    }

    private fun handleNavigation() {
        when {
            !accountManager.isAccountExisted() -> event(NavCreateAccountEvent)
            !accountManager.isAccountActivated() -> event(NavActivateAccountEvent)
            !accountManager.isStaySignedIn() -> event(NavSignInEvent)
            else -> event(NavHomeScreenEvent)
        }
    }

    private fun createRemoteSigner() {
        createSignerUseCase
            .execute(
                name = "TESTER",
                xpub = "xpub6Gs9Gp1P7ov2Xy6XmVBawLUwRgifGMK93K6bYuMdi9PfmJ6y6e7ffzD7JKCjWgJn71YGCQMozL1284Ywoaptv8UGRsua635k8yELEKk9nhh",
                publicKey = "0297da76f2b4ae426f41e617b4f13243716d1417d3acc3f8da7a54f301fc951741",
                derivationPath = "m/48'/0'/0'/7",
                masterFingerprint = "0b93c52e"
            )
            .defaultSchedulers()
            .doAfterTerminate(::getRemoteSigner)
            .subscribe({
                Log.i(TAG, "create signer success $it")
            }, {
                Log.e(TAG, "create signer error", it)
            })
            .addToDisposables()
    }

    private fun getRemoteSigner() {
        getRemoteSignerUseCase.execute()
            .defaultSchedulers()
            .subscribe({
                Log.i(TAG, "get remote signer $it")
            }, {
                Log.e(TAG, "get remote signer error", it)
            })
            .addToDisposables()
    }

    companion object {
        private const val TAG = "SplashViewModel"
    }
}