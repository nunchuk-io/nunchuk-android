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

package com.nunchuk.android.wallet.components.coin.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.usecase.GetTransactionUseCase
import com.nunchuk.android.usecase.coin.LockCoinUseCase
import com.nunchuk.android.usecase.coin.UnLockCoinUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoinDetailViewModel @Inject constructor(
    private val getTransactionUseCase: GetTransactionUseCase,
    private val lockCoinUseCase: LockCoinUseCase,
    private val unLockCoinUseCase: UnLockCoinUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CoinDetailFragmentArgs =
        CoinDetailFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CoinDetailEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CoinDetailUiState())
    val state = _state.asStateFlow()

    init {
        getTransactionDetail()
    }

    fun getTransactionDetail() {
        getTransactionUseCase.execute(args.walletId, args.output.txid, false)
            .onEach { transition ->
                _state.update { it.copy(transaction = transition.transaction) }
            }
            .launchIn(viewModelScope)
    }

    fun lockCoin(isLocked: Boolean) {
        viewModelScope.launch {
            val result = if (isLocked) lockCoinUseCase(
                LockCoinUseCase.Params(
                    groupId = assistedWalletManager.getGroupId(args.walletId),
                    walletId = args.walletId,
                    txId = args.output.txid,
                    vout = args.output.vout,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
            else unLockCoinUseCase(
                UnLockCoinUseCase.Params(
                    groupId = assistedWalletManager.getGroupId(args.walletId),
                    walletId = args.walletId,
                    txId = args.output.txid,
                    vout = args.output.vout,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(args.walletId)
                )
            )
            result.onSuccess {
                _event.emit(CoinDetailEvent.LockOrUnlockSuccess(isLocked))
            }.onFailure {
                _event.emit(CoinDetailEvent.ShowError(it.message.orEmpty()))
            }
        }
    }
}

sealed class CoinDetailEvent {
    data class ShowError(val message: String) : CoinDetailEvent()
    data class LockOrUnlockSuccess(val isLocked: Boolean) : CoinDetailEvent()
}