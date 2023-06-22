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

package com.nunchuk.android.auth.components.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.components.signin.SignInEvent.*
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.auth.util.orUnknownError
import com.nunchuk.android.auth.validator.doAfterValidate
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.retry.DEFAULT_RETRY_POLICY
import com.nunchuk.android.core.retry.RetryPolicy
import com.nunchuk.android.core.retry.retryIO
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
internal class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val accountManager: AccountManager,
    private val signInModeHolder: SignInModeHolder,
    private val getPrimaryKeyListUseCase: GetPrimaryKeyListUseCase,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    @Named(DEFAULT_RETRY_POLICY) private val retryPolicy: RetryPolicy
) : NunchukViewModel<Unit, SignInEvent>() {

    private var staySignedIn = false

    override val initialState = Unit

    private var token: String? = null
    private var encryptedDeviceId: String? = null

    init {
        viewModelScope.launch {
            clearInfoSessionUseCase(Unit)
        }
    }

    private fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) { event(EmailRequiredEvent) }
        !EmailValidator.valid(email) -> doAfterValidate(false) { event(EmailInvalidEvent) }
        else -> doAfterValidate { event(EmailValidEvent) }
    }

    private fun validatePassword(password: String) = when {
        password.isBlank() -> doAfterValidate(false) { event(PasswordRequiredEvent) }
        else -> doAfterValidate { event(PasswordValidEvent) }
    }

    fun handleSignIn(email: String, password: String) {
        if (validateEmail(email) && validatePassword(password)) {
            signInUseCase.execute(email = email, password = password, staySignedIn = staySignedIn)
                .retryIO(retryPolicy)
                .onStart { event(ProcessingEvent) }
                .flowOn(IO)
                .map {
                    token = it.first
                    encryptedDeviceId = it.second
                    fileLog(message = "start initNunchuk")
                    val result = initNunchuk()
                    fileLog(message = "end initNunchuk")
                    result
                }
                .onEach {
                    signInModeHolder.setCurrentMode(SignInMode.EMAIL)
                    event(
                        SignInSuccessEvent(
                            token = token.orEmpty(),
                            deviceId = encryptedDeviceId.orEmpty()
                        )
                    )
                }
                .flowOn(Main)
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
                        event(SignInErrorEvent(message = it.message.orUnknownError()))
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun checkPrimaryKeyAccounts() = viewModelScope.launch {
        val result = getPrimaryKeyListUseCase(Unit)
        if (result.isSuccess) {
            val data = result.getOrNull().orEmpty()
            if (data.isEmpty()) {
                event(CheckPrimaryKeyAccountEvent(arrayListOf()))
            } else {
                event(CheckPrimaryKeyAccountEvent(ArrayList(data)))
            }
        }
    }

    private suspend fun initNunchuk() = initNunchukUseCase(
        InitNunchukUseCase.Param(
            accountId = accountManager.getAccount().email
        )
    )

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }
}