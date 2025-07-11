/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.app.splash

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.guestmode.isGuestMode
import com.nunchuk.android.core.guestmode.isPrimaryKey
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetBiometricConfigUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.pin.GetCustomPinConfigFlowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder,
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val application: Application,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase,
    private val getCustomPinConfigFlowUseCase: GetCustomPinConfigFlowUseCase,
    private val getBiometricConfigUseCase: GetBiometricConfigUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _event = MutableSharedFlow<SplashEvent>(1)
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val mode = signInModeHolder.getCurrentMode()
            val account = accountManager.getAccount()
            val info = application.packageManager.getPackageInfo(application.packageName, 0)
            val isFreshInstall = info.firstInstallTime == info.lastUpdateTime
            val isAccountExisted = accountManager.isAccountExisted()
            val pin = getWalletPinUseCase(Unit).map { it.getOrDefault("") }.first()
            val settings = getWalletSecuritySettingUseCase(Unit)
                .map { it.getOrDefault(WalletSecuritySetting.DEFAULT) }
                .first()
            val isDecoyDisablePin = getCustomPinConfigFlowUseCase(account.decoyPin)
                .map { it.getOrDefault(true) }
                .first()
            val isBiometricEnable =
                getBiometricConfigUseCase(Unit).first().getOrNull()?.enabled == true
            val shouldAskPin = pin.isNotEmpty()
                    || (settings.protectWalletPassphrase && mode == SignInMode.PRIMARY_KEY)
                    || (settings.protectWalletPassword && mode == SignInMode.EMAIL)
            @Suppress("DEPRECATION")
            when {
                shouldAskPin && (mode == SignInMode.UNKNOWN || (mode == SignInMode.PRIMARY_KEY && accountManager.isStaySignedIn().not())) -> {
                    _event.emit(SplashEvent.NavUnlockPinScreenEvent)
                }

                isAccountExisted && accountManager.isStaySignedIn().not() -> {
                    val askPin = shouldAskPin && isDecoyDisablePin
                    val askBiometric = isBiometricEnable && mode.isGuestMode().not() && mode.isPrimaryKey().not()
                    _event.emit(SplashEvent.NavSignInEvent(askPin = askPin, askBiometric = askBiometric))
                }

                isAccountExisted && accountManager.isAccountActivated() || mode.isGuestMode() -> {
                    _event.emit(
                        SplashEvent.NavHomeScreenEvent(
                            askPin = shouldAskPin && isDecoyDisablePin,
                            askBiometric = isBiometricEnable && mode.isGuestMode()
                                .not() && mode.isPrimaryKey().not(),
                            isGuestMode = mode.isGuestMode()
                        )
                    )
                }
                // can delete after x version
                !isFreshInstall && !accountManager.isHasAccountBefore() && info.versionCode <= 245 -> {
                    accountManager.storeAccount(AccountInfo(loginType = SignInMode.GUEST_MODE.value))
                    _event.emit(
                        SplashEvent.NavHomeScreenEvent(
                            askPin = shouldAskPin && isDecoyDisablePin,
                            false,
                            isGuestMode = mode.isGuestMode()
                        )
                    )
                }

                else -> _event.emit(SplashEvent.NavSignInEvent(askPin = false, askBiometric = false))
            }
        }
    }
}