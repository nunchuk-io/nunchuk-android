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

package com.nunchuk.android.wallet.shared.components.config

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.usecase.CreateSharedWalletUseCase
import com.nunchuk.android.usecase.GetMatrixEventUseCase
import com.nunchuk.android.usecase.GetRoomWalletUseCase
import com.nunchuk.android.utils.onException
import com.nunchuk.android.wallet.shared.components.config.SharedWalletConfigEvent.CreateSharedWalletSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.getRoom
import org.matrix.android.sdk.api.session.room.members.RoomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import javax.inject.Inject

@HiltViewModel
internal class SharedWalletConfigViewModel @Inject constructor(
    private val createSharedWalletUseCase: CreateSharedWalletUseCase,
    private val getRoomWalletUseCase: GetRoomWalletUseCase,
    private val getMatrixEventUseCase: GetMatrixEventUseCase,
    private val accountManager: AccountManager,
    private val sessionHolder: SessionHolder
) : NunchukViewModel<SharedWalletConfigState, SharedWalletConfigEvent>() {

    override val initialState = SharedWalletConfigState()

    init {
        if (sessionHolder.hasActiveRoom()) {
            val roomMembers =
                sessionHolder.getSafeActiveSession()?.getRoom(sessionHolder.getActiveRoomId())
                    ?.membershipService()
                    ?.getRoomMembers(RoomMemberQueryParams.Builder().build())
            updateState { copy(signerModels = roomMembers.orEmpty().toSignerModels()) }
            getRoomWallet(sessionHolder.getActiveRoomId())
        }
    }

    private fun getRoomWallet(roomId: String) {
        viewModelScope.launch {
            getRoomWalletUseCase.execute(roomId).flowOn(Dispatchers.IO).onException { }
                .flowOn(Dispatchers.Main).collect {
                    updateState { copy(roomWallet = it) }
                    getMatrixEvent(it?.initEventId)
                }
        }
    }

    private fun getMatrixEvent(eventId: String?) {
        eventId ?: return
        viewModelScope.launch {
            val result = getMatrixEventUseCase(eventId)
            if (result.isSuccess) {
                val isSender = result.getOrThrow().sender == accountManager.getAccount().chatId
                updateState { copy(isSender = isSender) }
            }
        }
    }

    fun finalizeWallet() {
        viewModelScope.launch {
            val roomId = sessionHolder.getActiveRoomId()
            createSharedWalletUseCase.execute(roomId).flowOn(Dispatchers.IO).onException { }
                .collect {
                    getRoomWallet(roomId)
                    event(CreateSharedWalletSuccess)
                }
        }
    }

}

private fun List<RoomMemberSummary>.toSignerModels() = map(RoomMemberSummary::toSignerModel)

private fun RoomMemberSummary.toSignerModel() = SignerModel(
    id = userId, name = displayName ?: userId, fingerPrint = "", derivationPath = ""
)