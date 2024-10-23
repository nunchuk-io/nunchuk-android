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

package com.nunchuk.android.auth.components.changepass

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ChangePasswordSuccessError
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ChangePasswordSuccessEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordNotMatchedEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordRequiredEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ConfirmPasswordValidEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.LoadingEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.OldPasswordRequiredEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.OldPasswordValidEvent
import com.nunchuk.android.auth.components.changepass.ChangePasswordEvent.ShowEmailSentEvent
import com.nunchuk.android.auth.domain.ChangePasswordUseCase
import com.nunchuk.android.auth.domain.ResendPasswordUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val resendPasswordUseCase: ResendPasswordUseCase,
    private val signInUseCase: SignInUseCase,
    accountManager: AccountManager,
) : NunchukViewModel<Unit, ChangePasswordEvent>() {

    private val account = accountManager.getAccount()

    init {
        if (!account.activated) {
            event(ShowEmailSentEvent(account.email))
        }
    }

    override val initialState = Unit

    fun handleChangePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            if (validateOldPassword(oldPassword)
                && validateConfirmPassword(confirmPassword)
                && validateConfirmPasswordMatched(newPassword, confirmPassword)
            ) {
                changePasswordUseCase.execute(oldPassword = oldPassword, newPassword = newPassword)
                    .onStart {
                        if (account.token.isEmpty()) {
                            signInUseCase.execute(
                                email = account.email,
                                password = oldPassword,
                                staySignedIn = false,
                                fetchUserInfo = false
                            ).first()
                        }
                    }
                    .flowOn(IO)
                    .onStart { event(LoadingEvent) }
                    .onException { event(ChangePasswordSuccessError(it.message)) }
                    .flowOn(Main)
                    .collect { onChangePasswordSuccess() }
            }
        }
    }

    private fun onChangePasswordSuccess() {
        setEvent(ChangePasswordSuccessEvent)
    }

    private fun validateConfirmPasswordMatched(
        newPassword: String,
        confirmPassword: String
    ): Boolean {
        val matched = newPassword == confirmPassword
        if (!matched) {
            setEvent(ConfirmPasswordNotMatchedEvent)
        }
        return matched
    }

    private fun validateOldPassword(oldPassword: String) = when {
        oldPassword.isEmpty() -> doAfterValidate(false) { setEvent(OldPasswordRequiredEvent) }
        else -> doAfterValidate { setEvent(OldPasswordValidEvent) }
    }

    private fun validateConfirmPassword(confirmPassword: String) = when {
        confirmPassword.isEmpty() -> doAfterValidate(false) { setEvent(ConfirmPasswordRequiredEvent) }
        else -> doAfterValidate { setEvent(ConfirmPasswordValidEvent) }
    }

    fun resendPassword() {
        viewModelScope.launch {
            resendPasswordUseCase(account.email)
                .onSuccess {
                    setEvent(ChangePasswordEvent.ResendPasswordSuccessEvent(account.email))
                }.onFailure {
                    setEvent(ChangePasswordSuccessError(it.message))
                }
        }
    }

    companion object{
        const val SPECIAL_CHARACTERS = "!@#\$%^&*()_+[]{}|;:',.<>?/~`-="
    }
}