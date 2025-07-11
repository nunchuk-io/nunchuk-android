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

package com.nunchuk.android.signer.software.components.primarykey.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.SignInPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch

internal class PKeySignInViewModel @AssistedInject constructor(
    @Assisted private val args: PKeySignInArgs,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val signInPrimaryKeyUseCase: SignInPrimaryKeyUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    ) : NunchukViewModel<PKeySignInState, PKeySignInEvent>() {

    override val initialState: PKeySignInState = PKeySignInState(primaryKey = args.primaryKey)

    init {
        setupNunchuk()
    }

    private fun setupNunchuk() {
        viewModelScope.launch {
            try {
                val result = getAppSettingUseCase(Unit)
                val appSettingsNew = result.getOrThrow().copy(chain = args.primaryKey.chain!!)
                updateState { copy(appSettings = result.getOrThrow()) }
                updateAppSettingUseCase(appSettingsNew).getOrThrow()
                initNunchukUseCase(InitNunchukUseCase.Param(accountId = args.primaryKey.account)).getOrThrow()
            } catch (e: Exception) {
                setEvent(PKeySignInEvent.ProcessErrorEvent(e.message.orUnknownError()))
            }
        }
    }

    fun setStaySignedIn(staySignedIn: Boolean) = updateState { copy(staySignedIn = staySignedIn) }

    fun handleSignIn(passphrase: String) = viewModelScope.launch {
        setEvent(PKeySignInEvent.LoadingEvent(true))
        val result = signInPrimaryKeyUseCase(
            SignInPrimaryKeyUseCase.Param(
                passphrase = passphrase,
                address = args.primaryKey.address,
                signerName = args.primaryKey.name,
                username = args.primaryKey.account,
                masterFingerprint = args.primaryKey.masterFingerprint,
                staySignedIn = getState().staySignedIn
            )
        )
        val appSettings = state.value?.appSettings
        if (result.isFailure || appSettings == null) {
            setEvent(PKeySignInEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        } else {
            try {
                updateAppSettingUseCase(appSettings).getOrThrow()
                initNunchukUseCase(InitNunchukUseCase.Param(accountId = args.primaryKey.account)).getOrThrow()
                getUserProfileUseCase(Unit).onSuccess {
                    setEvent(PKeySignInEvent.SignInSuccessEvent)
                }
            } catch (e: Exception) {
                setEvent(PKeySignInEvent.InitFailure(e.message.orUnknownError()))
            }
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeySignInArgs): PKeySignInViewModel
    }
}