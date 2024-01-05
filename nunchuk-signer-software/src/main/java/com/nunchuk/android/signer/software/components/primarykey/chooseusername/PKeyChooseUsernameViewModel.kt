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

package com.nunchuk.android.signer.software.components.primarykey.chooseusername

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.domain.SignUpPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.usecase.GetMasterFingerprintUseCase
import com.nunchuk.android.usecase.GetPrimaryKeyAddressUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class PKeyChooseUsernameViewModel @AssistedInject constructor(
    @Assisted private val args: PKeyChooseUsernameArgs,
    private val getMasterFingerprintUseCase: GetMasterFingerprintUseCase,
    private val getPrimaryKeyAddressUseCase: GetPrimaryKeyAddressUseCase,
    private val signUpPrimaryKeyUseCase: SignUpPrimaryKeyUseCase,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val getTurnOnNotificationStoreUseCase: GetTurnOnNotificationStoreUseCase,
    private val updateTurnOnNotificationStoreUseCase: UpdateTurnOnNotificationStoreUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase
) : NunchukViewModel<PKeyChooseUsernameEventState, PKeyChooseUsernameEvent>() {

    override val initialState = PKeyChooseUsernameEventState()

    init {
        getDefaultUserName()
    }

    private fun getDefaultUserName() = viewModelScope.launch {
        setEvent(PKeyChooseUsernameEvent.LoadingEvent(true))
        val param = GetMasterFingerprintUseCase.Param(
            mnemonic = args.mnemonic,
            passphrase = args.passphrase
        )
        val result = getMasterFingerprintUseCase(param)
        setEvent(PKeyChooseUsernameEvent.LoadingEvent(false))
        if (result.isSuccess) {
            val data = result.getOrThrow() ?: return@launch
            updateState { copy(username = data, defaultUserName = data) }
            setEvent(PKeyChooseUsernameEvent.GetDefaultUsernameSuccess(data))
        } else {
            setEvent(PKeyChooseUsernameEvent.ProcessFailure(result.exceptionOrNull()?.message.orEmpty()))
        }
    }

    fun updateUsername(username: String) {
        updateState { copy(username = username) }
    }

    fun handleContinue() = viewModelScope.launch {
        val state = getState()
        if (state.username.trim().contains(" ")) {
            setEvent(PKeyChooseUsernameEvent.InvalidUsername)
            return@launch
        }
        setEvent(PKeyChooseUsernameEvent.LoadingEvent(true))
        val initNunchukResult =
            initNunchukUseCase(InitNunchukUseCase.Param(accountId = state.username))
        if (initNunchukResult.isFailure) {
            setEvent(PKeyChooseUsernameEvent.ProcessFailure(initNunchukResult.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        val param = GetPrimaryKeyAddressUseCase.Param(
            mnemonic = args.mnemonic,
            passphrase = args.passphrase
        )
        val addressResult = getPrimaryKeyAddressUseCase(param)
        if (addressResult.isFailure) {
            setEvent(PKeyChooseUsernameEvent.ProcessFailure(addressResult.exceptionOrNull()?.message.orUnknownError()))
            return@launch
        }
        val resultSignUp = signUpPrimaryKeyUseCase(
            SignUpPrimaryKeyUseCase.Param(
                mnemonic = args.mnemonic,
                passphrase = args.passphrase,
                address = addressResult.getOrThrow().orEmpty(),
                username = state.username,
                signerName = args.signerName,
                defaultUserName = state.defaultUserName.orEmpty(),
                staySignedIn = true
            )
        )
        if (resultSignUp.isSuccess) {
            getUserProfileUseCase(Unit).onSuccess {
                signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
                setEvent(PKeyChooseUsernameEvent.SignUpSuccess)
            }
        } else {
            setEvent(PKeyChooseUsernameEvent.ProcessFailure(resultSignUp.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getTurnOnNotification() = viewModelScope.launch {
        val result = getTurnOnNotificationStoreUseCase(Unit)
        var isTurnOn = false
        if (result.isSuccess) {
            isTurnOn = result.getOrDefault(false)
        }
        setEvent(PKeyChooseUsernameEvent.GetTurnOnNotificationSuccess(isTurnOn))
    }

    fun updateTurnOnNotification() = viewModelScope.launch {
        updateTurnOnNotificationStoreUseCase(UpdateTurnOnNotificationStoreUseCase.Param(false))
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeyChooseUsernameArgs): PKeyChooseUsernameViewModel
    }
}