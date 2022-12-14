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
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.domain.GetTapSignerStatusByIdUseCase
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTargetAction
import com.nunchuk.android.core.domain.membership.VerifiedPasswordTokenUseCase
import com.nunchuk.android.core.guestmode.SignInMode
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.messages.usecase.message.LeaveRoomUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.RoomWallet
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.model.joinKeys
import com.nunchuk.android.share.GetContactsUseCase
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.DeleteWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.utils.retrieveInfo
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameErrorEvent
import com.nunchuk.android.wallet.components.config.WalletConfigEvent.UpdateNameSuccessEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class WalletConfigViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val leaveRoomUseCase: LeaveRoomUseCase,
    private val accountManager: AccountManager,
    private val verifiedPasswordTokenUseCase: VerifiedPasswordTokenUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val getTapSignerStatusByIdUseCase: GetTapSignerStatusByIdUseCase,
    getContactsUseCase: GetContactsUseCase,
) : NunchukViewModel<WalletConfigState, WalletConfigEvent>() {

    lateinit var walletId: String

    private val contacts = getContactsUseCase.execute()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun init(walletId: String) {
        this.walletId = walletId
        getWalletDetails()
    }

    private fun getWalletDetails() {
        viewModelScope.launch {
            getWalletUseCase.execute(walletId)
                .onEach {
                    if (it.roomWallet != null) {
                        loadContact()
                    }
                }
                .map {
                    WalletConfigState(
                        walletExtended = it,
                        signers = mapSigners(it.wallet.signers, it.roomWallet),
                        isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                    )
                }
                .flowOn(Dispatchers.IO)
                .onException { event(UpdateNameErrorEvent(it.message.orUnknownError())) }
                .flowOn(Dispatchers.Main)
                .collect { updateState { it } }
        }
    }

    private fun loadContact() {
        viewModelScope.launch {
            contacts.collect {
                mapSigners(
                    getState().walletExtended.wallet.signers,
                    getState().walletExtended.roomWallet
                )
            }
        }
    }

    fun verifyPassword(password: String, xfp: String) {
        viewModelScope.launch {
            setEvent(WalletConfigEvent.Loading(true))
            val result = verifiedPasswordTokenUseCase(
                VerifiedPasswordTokenUseCase.Param(
                    VerifiedPasswordTargetAction.UPDATE_SERVER_KEY.name,
                    password
                )
            )
            setEvent(WalletConfigEvent.Loading(false))
            if (result.isSuccess) {
                setEvent(
                    WalletConfigEvent.VerifyPasswordSuccess(
                        result.getOrThrow().orEmpty(),
                        xfp
                    )
                )
            } else {
                setEvent(WalletConfigEvent.WalletDetailsError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleEditCompleteEvent(walletName: String) {
        viewModelScope.launch {
            val newWallet = getState().walletExtended.wallet.copy(name = walletName)
            updateWalletUseCase.execute(
                newWallet,
                assistedWalletManager.isActiveAssistedWallet(walletId)
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

    private suspend fun mapSigners(
        singleSigners: List<SingleSigner>,
        roomWallet: RoomWallet? = null
    ): List<SignerModel> {
        val account = accountManager.getAccount()
        val signers =
            singleSigners.map { it.toModel(isPrimaryKey = isPrimaryKey(it.masterSignerId)) }
                .map { signer ->
                    if (signer.type == SignerType.NFC) signer.copy(
                        cardId = getTapSignerStatusByIdUseCase(
                            signer.id
                        ).getOrNull()?.ident.orEmpty()
                    )
                    else signer
                }

        return roomWallet?.joinKeys()?.map { key ->
            key.retrieveInfo(
                key.chatId == account.chatId, signers, contacts.value
            )
        } ?: signers
    }

    private fun isPrimaryKey(id: String) =
        accountManager.loginType() == SignInMode.PRIMARY_KEY.value && accountManager.getPrimaryKeyInfo()?.xfp == id

    fun isAssistedWallet() = assistedWalletManager.isActiveAssistedWallet(walletId)

    fun isInactiveAssistedWallet() = assistedWalletManager.isInactiveAssistedWallet(walletId)

    override val initialState: WalletConfigState
        get() = WalletConfigState()
}