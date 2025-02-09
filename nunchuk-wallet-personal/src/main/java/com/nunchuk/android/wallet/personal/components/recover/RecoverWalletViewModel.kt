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

package com.nunchuk.android.wallet.personal.components.recover

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.ImportWalletUseCase
import com.nunchuk.android.usecase.UpdateWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RecoverWalletViewModel @Inject constructor(
    private val importWalletUseCase: ImportWalletUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val getWalletUseCase: GetWalletUseCase,
) : NunchukViewModel<RecoverWalletState, RecoverWalletEvent>() {

    override val initialState = RecoverWalletState()

    val walletName: String?
        get() = state.value?.walletName

    init {
        updateState { initialState }
    }

    fun importWallet(filePath: String, name: String, description: String) {
        viewModelScope.launch {
            importWalletUseCase.execute(filePath, name, description)
                .flowOn(Dispatchers.IO)
                .onException { event(RecoverWalletEvent.ImportWalletErrorEvent(it.readableMessage())) }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(RecoverWalletEvent.ImportWalletSuccessEvent(it.id, it.name))
                }
        }
    }

    fun updateWalletName(walletName: String) {
        updateState { copy(walletName = walletName) }
    }

    fun updateWallet(walletId: String, walletName: String) {
        getWalletUseCase.execute(walletId)
            .flowOn(Dispatchers.IO)
            .onException { event(RecoverWalletEvent.UpdateWalletErrorEvent(it.message.orEmpty())) }
            .map {
                updateWalletUseCase(
                    UpdateWalletUseCase.Params(
                        it.wallet.copy(name = walletName)
                    )
                ).onFailure { err ->
                    event(RecoverWalletEvent.UpdateWalletErrorEvent(err.message.orEmpty()))
                }
            }.onEach {
                event(RecoverWalletEvent.UpdateWalletSuccessEvent(walletId, walletName))
            }.flowOn(Dispatchers.Main).launchIn(viewModelScope)
    }

    fun handleContinueEvent() {
        val currentState = getState()
        if (currentState.walletName.isNotEmpty()) {
            event(RecoverWalletEvent.WalletSetupDoneEvent(walletName = currentState.walletName))
        } else {
            event(RecoverWalletEvent.WalletNameRequiredEvent)
        }
    }

}