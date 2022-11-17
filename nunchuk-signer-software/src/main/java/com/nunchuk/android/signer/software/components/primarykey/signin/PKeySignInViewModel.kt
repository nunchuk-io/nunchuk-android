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

package com.nunchuk.android.signer.software.components.primarykey.signin

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.SignInPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

internal class PKeySignInViewModel @AssistedInject constructor(
    @Assisted private val args: PKeySignInArgs,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val signInPrimaryKeyUseCase: SignInPrimaryKeyUseCase,
    private val signInModeHolder: SignInModeHolder,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NunchukViewModel<PKeySignInState, PKeySignInEvent>() {

    override val initialState: PKeySignInState =
        PKeySignInState(primaryKey = args.primaryKey)

    init {
        setupNunchuk()
    }

    private fun setupNunchuk() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flatMapConcat {
                    postState { copy(appSettings = it) }
                    val appSettingsNew = it.copy(chain = args.primaryKey.chain!!)
                    updateAppSettingUseCase.execute(appSettingsNew)
                }
                .flatMapConcat {
                    initNunchukUseCase.execute(accountId = args.primaryKey.account)
                        .onException {
                            event(PKeySignInEvent.InitFailure(it.message.orUnknownError()))
                        }
                }
                .flowOn(ioDispatcher)
                .onException { }
                .collect {}
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
            setEvent(PKeySignInEvent.LoadingEvent(false))
            setEvent(PKeySignInEvent.ProcessErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        } else {
            updateAppSettingUseCase.execute(appSettings)
                .flatMapConcat {
                    initNunchukUseCase.execute(accountId = args.primaryKey.account)
                        .onException {
                            event(PKeySignInEvent.InitFailure(it.message.orUnknownError()))
                        }
                }
                .flowOn(ioDispatcher)
                .onException {}
                .collect {
                    setEvent(PKeySignInEvent.LoadingEvent(false))
                    signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
                    setEvent(PKeySignInEvent.SignInSuccessEvent)
                }
        }
    }

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeySignInArgs): PKeySignInViewModel
    }
}