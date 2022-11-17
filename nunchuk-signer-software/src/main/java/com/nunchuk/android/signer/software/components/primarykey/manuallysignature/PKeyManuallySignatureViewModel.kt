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

package com.nunchuk.android.signer.software.components.primarykey.manuallysignature

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.domain.PostNoncePrimaryKeyUseCase
import com.nunchuk.android.core.domain.SignInManuallyPrimaryKeyUseCase
import com.nunchuk.android.core.domain.UpdateTurnOnNotificationStoreUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.share.InitNunchukUseCase
import com.nunchuk.android.utils.onException
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class PKeyManuallySignatureViewModel @AssistedInject constructor(
    @Assisted private val args: PKeyManuallySignatureArgs,
    private val initNunchukUseCase: InitNunchukUseCase,
    private val signInManuallyPrimaryKeyUseCase: SignInManuallyPrimaryKeyUseCase,
    private val postNoncePrimaryKeyUseCase: PostNoncePrimaryKeyUseCase,
    private val signInModeHolder: SignInModeHolder,
    private val getTurnOnNotificationStoreUseCase: GetTurnOnNotificationStoreUseCase,
    private val updateTurnOnNotificationStoreUseCase: UpdateTurnOnNotificationStoreUseCase
) : NunchukViewModel<PKeyManuallySignatureState, PKeyManuallySignatureEvent>() {

    init {
        getChallengeMessage()
    }

    fun updateSignature(signature: String) {
        updateState { copy(signature = signature) }
    }

    fun handleSignIn() = viewModelScope.launch {
        val signature = getState().signature
        if (signature.isNullOrBlank()) return@launch
        setEvent(PKeyManuallySignatureEvent.LoadingEvent(true))
        val result = signInManuallyPrimaryKeyUseCase(
            SignInManuallyPrimaryKeyUseCase.Param(
                username = args.username,
                signature = signature
            )
        )
        if (result.isSuccess) {
            initNunchukUseCase.execute(accountId = args.username)
                .flowOn(Dispatchers.IO)
                .onException { setEvent(PKeyManuallySignatureEvent.ProcessFailure(it.message.orUnknownError())) }
                .collect {
                    signInModeHolder.setCurrentMode(SignInMode.PRIMARY_KEY)
                    setEvent(PKeyManuallySignatureEvent.LoadingEvent(false))
                    setEvent(PKeyManuallySignatureEvent.SignInSuccess)
                }
        } else {
            setEvent(PKeyManuallySignatureEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun getChallengeMessage() = viewModelScope.launch {
        val username = args.username
        setEvent(PKeyManuallySignatureEvent.LoadingEvent(true))
        val result =
            postNoncePrimaryKeyUseCase(
                PostNoncePrimaryKeyUseCase.Param(
                    username = username,
                    address = null,
                    nonce = null
                )
            )
        setEvent(PKeyManuallySignatureEvent.LoadingEvent(false))
        if (result.isSuccess) {
            updateState { copy(challengeMessage = result.getOrThrow()) }
        } else {
            setEvent(PKeyManuallySignatureEvent.ProcessFailure(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun updateStaySignedIn(staySignedIn: Boolean) {
        updateState { copy(staySignedIn = staySignedIn) }
    }

    fun getTurnOnNotification() = viewModelScope.launch {
        val result = getTurnOnNotificationStoreUseCase(Unit)
        var isTurnOn = false
        if (result.isSuccess) {
            isTurnOn = result.getOrDefault(false)
        }
        setEvent(PKeyManuallySignatureEvent.GetTurnOnNotificationSuccess(isTurnOn))
    }

    fun updateTurnOnNotification() = viewModelScope.launch {
        updateTurnOnNotificationStoreUseCase(UpdateTurnOnNotificationStoreUseCase.Param(false))
    }

    override val initialState: PKeyManuallySignatureState =
        PKeyManuallySignatureState(username = args.username)

    @AssistedFactory
    internal interface Factory {
        fun create(args: PKeyManuallySignatureArgs): PKeyManuallySignatureViewModel
    }
}