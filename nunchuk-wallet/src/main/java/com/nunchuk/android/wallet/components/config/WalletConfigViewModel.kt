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

package com.nunchuk.android.wallet.components.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.auth.domain.SignInUseCase
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val accountManager: AccountManager,
    private val signInUseCase: SignInUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
) : NunchukViewModel<WalletConfigState, WalletConfigEvent>() {

    lateinit var walletId: String

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .map { WalletConfigState(it, mapSigners(it.wallet.signers)) }
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { updateState { it } }
        }
    }

    fun signIn(password: String, xfp: String) {
        val account = accountManager.getAccount()
        viewModelScope.launch {
            signInUseCase.execute(
                email = account.email,
                password = password,
                staySignedIn = account.staySignedIn
            ).onStart { setEvent(WalletConfigEvent.Loading(true)) }
                .onCompletion { setEvent(WalletConfigEvent.Loading(false)) }
                .onException {
                    setEvent(WalletConfigEvent.WalletDetailsError(it.message.orUnknownError()))
                }.collect {
                    setEvent(WalletConfigEvent.VerifyPasswordSuccess(xfp))
                }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            val newWallet = getState().walletExtended.wallet.copy(name = walletName)
            updateWalletUseCase.execute(
                newWallet,
                assistedWalletManager.isAssistedWallet(walletId)
            )
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState { copy(walletExtended = walletExtended.copy(wallet = newWallet)) }
                    event(UpdateNameSuccessEvent)
                }
        }
    }

    private fun showError(t: Throwable) {
        event(WalletConfigEvent.WalletDetailsError(t.message.orUnknownError()))
    }

    private suspend fun leaveRoom(onDone: suspend () -> Unit) {
        val roomId = getState().walletExtended.roomWallet?.roomId
        if (roomId == null) {
            onDone()
            return
        }
        leaveRoomUseCase.execute(roomId)
            .flowOn(Dispatchers.IO)
            .onException { e -> showError(e) }
            .collect {
                onDone()
            }
    }

    fun handleDeleteWallet() {
        viewModelScope.launch {
            leaveRoom {
                when (val event = deleteWalletUseCase.execute(walletId)) {
                    is Result.Success -> event(WalletConfigEvent.DeleteWalletSuccess)
                    is Result.Error -> showError(event.exception)
                }
            }
        }
    }

    fun isSharedWallet() = getState().walletExtended.isShared

    private suspend fun mapSigners(singleSigners: List<SingleSigner>): List<SignerModel> {
        return singleSigners.map { it.toModel(isPrimaryKey = isPrimaryKey(it.masterSignerId)) }
            .map { signer ->
                if (signer.type == SignerType.NFC) signer.copy(
                    cardId = getTapSignerStatusByIdUseCase(
                        signer.id
                    ).getOrNull()?.ident.orEmpty()
                )
                else signer
            }
    }

    private fun isPrimaryKey(id: String) =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    override val initialState: WalletConfigState
        get() = WalletConfigState()
}