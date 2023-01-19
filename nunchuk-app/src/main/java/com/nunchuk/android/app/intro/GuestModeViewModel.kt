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

package com.nunchuk.android.app.intro

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.app.splash.GuestModeEvent
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class GuestModeViewModel @Inject constructor(
    private val initNunchukUseCase: InitNunchukUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val accountManager: AccountManager
) : NunchukViewModel<Unit, GuestModeEvent>() {

    override val initialState = Unit

    fun initGuestModeNunchuk() {
        viewModelScope.launch {
            setEvent(GuestModeEvent.LoadingEvent(true))
            val result = initNunchukUseCase(InitNunchukUseCase.Param(accountId = ""))
            setEvent(GuestModeEvent.LoadingEvent(false))
            if (result.isSuccess) {
                accountManager.removeAccount()
                signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
                event(GuestModeEvent.InitSuccessEvent)
            } else {
                event(GuestModeEvent.InitErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}