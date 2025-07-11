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

package com.nunchuk.android.auth.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.ConfirmPasswordNotMatchedEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.ConfirmPasswordRequiredEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.ConfirmPasswordValidEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.LoadingEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.NewPasswordRequiredEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.NewPasswordValidEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.OldPasswordRequiredEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.OldPasswordValidEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.RecoverPasswordErrorEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.RecoverPasswordSuccessEvent
import com.nunchuk.android.auth.components.recover.RecoverPasswordEvent.SignInErrorEvent
import com.nunchuk.android.auth.domain.RecoverPasswordUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.retry.DEFAULT_RETRY_POLICY
import com.nunchuk.android.core.retry.RetryPolicy
import com.nunchuk.android.core.retry.retryIO
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
internal class RecoverPasswordViewModel @Inject constructor(
    private val recoverPasswordUseCase: RecoverPasswordUseCase,
    private val signInUseCase: SignInUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    @Named(DEFAULT_RETRY_POLICY) private val retryPolicy: RetryPolicy,
    private val signInModeHolder: SignInModeHolder,
) : NunchukViewModel<Unit, RecoverPasswordEvent>() {

    override val initialState = Unit
    lateinit var email: String
    private var token: String? = null
    private var encryptedDeviceId: String? = null

    fun initData(email: String) {
        this.email = email
    }

    fun handleChangePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (validateOldPassword(oldPassword)
                && validateNewPassword(newPassword)
                && validateConfirmPassword(confirmPassword)
                && validateConfirmPasswordMatched(newPassword, confirmPassword)
            ) {
                event(LoadingEvent)
                val result = recoverPasswordUseCase.execute(
                    emailAddress = email,
                    oldPassword = oldPassword,
                    newPassword = newPassword
                )
                if (result is Success) {
                    signIn(newPassword)
                } else if (result is Error) {
                    event(RecoverPasswordErrorEvent(result.exception.message))
                }
            }
        }
    }

    private suspend fun signIn(password: String) {
        signInUseCase.execute(email = email, password = password, staySignedIn = true)
            .retryIO(retryPolicy)
            .onStart { event(LoadingEvent) }
            .flowOn(Dispatchers.IO)
            .map {
                token = it.token
                encryptedDeviceId = it.deviceId
                fileLog(message = "start initNunchuk")
                val result = initNunchuk()
                fileLog(message = "end initNunchuk")
                result
            }
            .onEach {
                event(
                    RecoverPasswordSuccessEvent(
                        token = token.orEmpty(),
                        deviceId = encryptedDeviceId.orEmpty()
                    )
                )
            }
            .flowOn(Dispatchers.Main)
            .onException {
                if (it is NunchukApiException) {
                    event(
                        SignInErrorEvent(
                            code = it.code,
                            message = it.message,
                            errorDetail = it.errorDetail
                        )
                    )
                } else {
                    event(RecoverPasswordErrorEvent(it.message.orUnknownError()))
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun initNunchuk() = initNunchukUseCase(
        InitNunchukUseCase.Param(
            accountId = email
        )
    )

    private fun validateConfirmPasswordMatched(
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        val matched = newPassword == confirmPassword
        if (!matched) {
            event(ConfirmPasswordNotMatchedEvent)
        }
        return matched
    }

    private fun validateOldPassword(oldPassword: String) = when {
        oldPassword.isEmpty() -> doAfterValidate(false) { event(OldPasswordRequiredEvent) }
        else -> doAfterValidate { event(OldPasswordValidEvent) }
    }

    private fun validateNewPassword(newPassword: String) = when {
        newPassword.isEmpty() -> doAfterValidate(false) { event(NewPasswordRequiredEvent) }
        else -> doAfterValidate { event(NewPasswordValidEvent) }
    }

    private fun validateConfirmPassword(confirmPassword: String) = when {
        confirmPassword.isEmpty() -> doAfterValidate(false) { event(ConfirmPasswordRequiredEvent) }
        else -> doAfterValidate { event(ConfirmPasswordValidEvent) }
    }

}