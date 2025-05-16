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

package com.nunchuk.android.auth.components.verify

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.ProcessingEvent
import com.nunchuk.android.auth.components.verify.VerifyNewDeviceEvent.SignInSuccessEvent
import com.nunchuk.android.auth.domain.ResendVerifyNewDeviceCodeUseCase
import com.nunchuk.android.auth.domain.VerifyNewDeviceUseCase
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class VerifyNewDeviceViewModel @Inject constructor(
    private val verifyNewDeviceUseCase: VerifyNewDeviceUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder,
    private val resendVerifyNewDeviceCodeUseCase: ResendVerifyNewDeviceCodeUseCase,
) : NunchukViewModel<Unit, VerifyNewDeviceEvent>() {

    override val initialState = Unit

    fun handleVerifyNewDevice(
        email: String,
        loginHalfToken: String,
        pin: String,
        deviceId: String,
        staySignedIn: Boolean
    ) {
        viewModelScope.launch {
            verifyNewDeviceUseCase.execute(
                email = email,
                loginHalfToken = loginHalfToken,
                pin = pin,
                deviceId = deviceId,
                staySignedIn = staySignedIn
            ).flowOn(Dispatchers.IO)
                .onStart { event(ProcessingEvent) }
                .map {
                    initNunchuk()
                }
                .flowOn(Dispatchers.Main)
                .onException { setEvent(VerifyNewDeviceEvent.ProcessErrorEvent(message = it.message.orUnknownError())) }
                .collect {
                    signInModeHolder.setCurrentMode(SignInMode.EMAIL)
                    event(SignInSuccessEvent)
                }
        }
    }

    fun handleResendVerifyNewDeviceCode(
        email: String,
        loginHalfToken: String,
        deviceId: String,
    ) = viewModelScope.launch {
        event(ProcessingEvent)
        val data = ResendVerifyNewDeviceCodeUseCase.Data(
            email = email,
            loginHalfToken = loginHalfToken,
            deviceId = deviceId
        )
        val result = resendVerifyNewDeviceCodeUseCase(data)
        if (result.isSuccess) {
            event(VerifyNewDeviceEvent.ResendVerifyCodeSuccessEvent)
        } else {
            event(VerifyNewDeviceEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private suspend fun initNunchuk() {
        val account = accountManager.getAccount()
        initNunchukUseCase(InitNunchukUseCase.Param(accountId = account.email)).getOrThrow()
    }
}