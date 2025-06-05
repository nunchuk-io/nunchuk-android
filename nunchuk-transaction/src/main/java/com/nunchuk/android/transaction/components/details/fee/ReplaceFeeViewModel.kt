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

package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.isValueKeySetDisable
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.EstimateFeeUseCase
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReplaceFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(ReplaceFeeState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<ReplaceFeeEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            val result = estimateFeeUseCase(Unit)
            if (result.isSuccess) {
                _state.update { it.copy(fee = result.getOrThrow()) }
            }
        }
        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    _state.update {
                        it.copy(antiFeeSniping = result.getOrDefault(false))
                    }
                }
        }
    }

    fun getWalletDetail(walletId: String) {
        viewModelScope.launch {
            getWalletDetail2UseCase(walletId).onSuccess { wallet ->
                _state.update {
                    it.copy(isValueKeySetDisable = wallet.isValueKeySetDisable)
                }
            }
        }
    }

    fun setPreviousFeeRate(previousFeeRate: Int) {
        _state.update {
            it.copy(previousFeeRate = previousFeeRate)
        }
    }

    fun onFeeChange(oldTx: Transaction, walletId: String, newFee: Int) {
        viewModelScope.launch {
            draftTransactionUseCase(
                DraftTransactionUseCase.Params(
                    walletId = walletId,
                    inputs = oldTx.inputs,
                    outputs = oldTx.userOutputs.associate { it.first to it.second },
                    subtractFeeFromAmount = oldTx.subtractFeeFromAmount,
                    feeRate = newFee.toManualFeeRate(),
                    replaceTxId = oldTx.txId
                )
            ).onSuccess { transaction ->
                _state.update {
                    it.copy(
                        cpfpFee = transaction.cpfpFee,
                        scriptPathFee = transaction.scriptPathFee
                    )
                }
            }.onFailure { exception ->
                _event.emit(ReplaceFeeEvent.ShowError(exception))
            }
        }
    }

    fun draftTransaction(oldTx: Transaction, walletId: String, newFee: Int) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))

            draftTransactionUseCase(
                DraftTransactionUseCase.Params(
                    walletId = walletId,
                    inputs = oldTx.inputs,
                    outputs = oldTx.userOutputs.associate { it.first to it.second },
                    subtractFeeFromAmount = oldTx.subtractFeeFromAmount,
                    feeRate = newFee.toManualFeeRate(),
                    replaceTxId = oldTx.txId
                )
            ).onSuccess { transaction ->
                _event.emit(ReplaceFeeEvent.DraftTransactionSuccess(transaction, newFee))
            }.onFailure { exception ->
                _event.emit(ReplaceFeeEvent.ShowError(exception))
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun onAntiFeeSnipingChange() {
        _state.update {
            it.copy(antiFeeSniping = !it.antiFeeSniping)
        }
    }
}
