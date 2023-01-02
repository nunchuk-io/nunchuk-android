/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SplashViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder,
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
    }

    fun initFlow() {
        viewModelScope.launch {
            val account = accountManager.getAccount()
            when {
                accountManager.isAccountExisted()
                        && accountManager.isAccountActivated().not() -> event(
                    SplashEvent.NavActivateAccountEvent
                )
                accountManager.isHasAccountBefore()
                        && accountManager.isStaySignedIn().not() -> event(
                    SplashEvent.NavSignInEvent
                )
                else -> event(
                    SplashEvent.NavHomeScreenEvent(
                        account.token, account.deviceId
                    )
                )
            }
        }
    }
}