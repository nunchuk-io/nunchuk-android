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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailInvalidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailRequiredEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.EmailValidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.PasswordRequiredEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.PasswordValidEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.ProcessingEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.SignInErrorEvent
import com.nunchuk.android.auth.components.signin.SignInEvent.SignInSuccessEvent
import com.nunchuk.android.auth.domain.CheckEmailAvailabilityUseCase
import com.nunchuk.android.auth.domain.RegisterUseCase
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.core.account.AccountInfo
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.account.SignInType
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.network.NunchukApiException
import com.nunchuk.android.core.profile.UpdateUseProfileUseCase
import com.nunchuk.android.core.retry.DEFAULT_RETRY_POLICY
import com.nunchuk.android.core.retry.RetryPolicy
import com.nunchuk.android.core.retry.retryIO
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.log.fileLog
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.GetPrimaryKeyListUseCase
import com.nunchuk.android.utils.EmailValidator
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
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
    @Named(DEFAULT_RETRY_POLICY) private val retryPolicy: RetryPolicy,
    private val checkEmailAvailabilityUseCase: CheckEmailAvailabilityUseCase,
    private val registerUseCase: RegisterUseCase,
    private val updateUseProfileUseCase: UpdateUseProfileUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _event = MutableSharedFlow<SignInEvent>()
    val event = _event.asSharedFlow()

    private var staySignedIn = false
    val type = savedStateHandle.get<SignInType>(EXTRA_TYPE) ?: SignInType.EMAIL
    private val _state =
        MutableStateFlow(SignInUiState(type = type))
    val state = _state.asStateFlow()

    private var token: String? = null
    private var encryptedDeviceId: String? = null

    init {
        if (type != SignInType.EMAIL) {
            _state.update { state -> state.copy(email = accountManager.getAccount().email) }
        }
        viewModelScope.launch {
            val isSignOut = savedStateHandle.get<Boolean>(EXTRA_SIGN_OUT) ?: true
            if (isSignOut) {
                clearInfoSessionUseCase(Unit)
            }
        }
        checkPrimaryKeyAccounts()
    }

    private suspend fun validateEmail(email: String) = when {
        email.isBlank() -> doAfterValidate(false) {
            _event.emit(EmailRequiredEvent)
        }

        !EmailValidator.valid(email) -> doAfterValidate(false) {
            _event.emit(EmailInvalidEvent)
        }

        else -> doAfterValidate {
            _event.emit(EmailValidEvent)
        }
    }

    private suspend fun validatePassword(password: String) = when {
        password.isBlank() -> doAfterValidate(false) {
            _event.emit(PasswordRequiredEvent)
        }

        else -> doAfterValidate {
            _event.emit(PasswordValidEvent)
        }
    }

    private suspend fun validateName(name: String) = when {
        name.isBlank() -> doAfterValidate(false) {
            _event.emit(SignInEvent.NameRequiredEvent)
        }

        else -> doAfterValidate {
            _event.emit(SignInEvent.NameValidEvent)
        }
    }

    fun onContinueClicked(email: String, password: String, name: String) {
        val stage = savedStateHandle.get<SignInType>(EXTRA_TYPE) ?: SignInType.EMAIL
        when (stage) {
            SignInType.EMAIL, SignInType.GUEST -> handleCheckEmail(email)
            SignInType.PASSWORD -> handleSignIn(email, password)
            SignInType.NAME -> handleUpdateName(name)
        }
    }

    private fun handleUpdateName(name: String) {
        viewModelScope.launch {
            if (validateName(name)) {
                updateUseProfileUseCase(UpdateUseProfileUseCase.Params(name = name))
                    .onSuccess {
                        _event.emit(SignInSuccessEvent)
                    }
                    .onFailure {
                        _event.emit(SignInErrorEvent(message = it.message.orUnknownError()))
                    }
            }
        }
    }

    private fun handleCheckEmail(email: String) {
        viewModelScope.launch {
            if (validateEmail(email)) {
                checkEmailAvailabilityUseCase(email)
                    .onSuccess {
                        if (!it.available) {
                            if (it.activated) {
                                setType(SignInType.PASSWORD)
                                _state.update { state -> state.copy(isSubscriberUser = it.hasSubscription) }
                            } else {
                                _event.emit(SignInEvent.RequireChangePassword(isNew = false))
                            }
                        } else {
                            register(email)
                        }
                    }.onFailure {
                        if (it is NunchukApiException && it.code == 404) {
                            register(email)
                        } else {
                            _event.emit(SignInErrorEvent(message = it.message.orUnknownError()))
                        }
                    }
            }
        }
    }

    private fun register(email: String) {
        viewModelScope.launch {
            registerUseCase.execute(email, email)
                .onSuccess {
                    _event.emit(SignInEvent.RequireChangePassword(isNew = true))
                }
                .onFailure {
                    _event.emit(SignInErrorEvent(message = it.message.orUnknownError()))
                }
        }
    }

    private fun handleSignIn(email: String, password: String) {
        viewModelScope.launch {
            if (validateEmail(email) && validatePassword(password)) {
                signInUseCase.execute(
                    email = email,
                    password = password,
                    staySignedIn = staySignedIn
                ).retryIO(retryPolicy)
                    .onStart { _event.emit(ProcessingEvent()) }
                    .flowOn(IO)
                    .map {
                        token = it.token
                        encryptedDeviceId = it.deviceId
                        fileLog(message = "start initNunchuk")
                        initNunchuk()
                        fileLog(message = "end initNunchuk")
                        it
                    }
                    .onEach {
                        signInModeHolder.setCurrentMode(SignInMode.EMAIL)
                        if (it.name == it.email) {
                            _event.emit(ProcessingEvent(false))
                            setType(SignInType.NAME)
                        } else {
                            _event.emit(SignInSuccessEvent)
                        }
                    }
                    .flowOn(Main)
                    .onException {
                        if (it is NunchukApiException) {
                            _event.emit(
                                SignInErrorEvent(
                                    code = it.code,
                                    message = it.message,
                                    errorDetail = it.errorDetail
                                )
                            )
                        } else {
                            _event.emit(SignInErrorEvent(message = it.message.orUnknownError()))
                        }
                    }
                    .collect {}
            }
        }
    }

    private fun checkPrimaryKeyAccounts() = viewModelScope.launch {
        getPrimaryKeyListUseCase(Unit)
            .onSuccess { data ->
                _state.update { state -> state.copy(accounts = data) }
            }
    }

    fun initGuestModeNunchuk() {
        viewModelScope.launch {
            initNunchukUseCase(InitNunchukUseCase.Param(accountId = ""))
                .onSuccess {
                    accountManager.storeAccount(AccountInfo().copy(loginType = SignInMode.GUEST_MODE.value))
                    signInModeHolder.setCurrentMode(SignInMode.GUEST_MODE)
                    _event.emit(SignInSuccessEvent)
                }.onFailure {
                    _event.emit(SignInErrorEvent(message = it.message.orUnknownError()))
                }
        }
    }

    private suspend fun initNunchuk() = initNunchukUseCase(
        InitNunchukUseCase.Param(
            accountId = accountManager.getAccount().email
        )
    )

    private suspend fun doAfterValidate(
        result: Boolean = true,
        func: suspend () -> Unit = {}
    ): Boolean {
        func()
        return result
    }

    fun storeStaySignedIn(staySignedIn: Boolean) {
        this.staySignedIn = staySignedIn
    }

    fun setType(stage: SignInType) {
        savedStateHandle[EXTRA_TYPE] = stage
        _state.update { state -> state.copy(type = stage) }
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_SIGN_OUT = "sign_out"
    }
}