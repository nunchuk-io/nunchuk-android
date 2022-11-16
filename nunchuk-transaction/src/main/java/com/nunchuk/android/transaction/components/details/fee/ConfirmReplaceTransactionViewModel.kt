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

package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.ReplaceTransactionUseCase
import com.nunchuk.android.model.Result
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.usecase.DraftTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmReplaceTransactionViewModel @Inject constructor(
    private val replaceTransactionUseCase: ReplaceTransactionUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<ReplaceFeeEvent>()
    private val _state = MutableStateFlow<ConfirmReplaceTransactionState?>(null)
    val state = _state.asStateFlow().filterIsInstance<ConfirmReplaceTransactionState>()
    val event = _event.asSharedFlow()

    fun draftTransaction(walletId: String, oldTx: Transaction, newFee: Int) {
        viewModelScope.launch {
            delay(150) // work around shared flow not show loading
            _event.emit(ReplaceFeeEvent.Loading(true))
            when (val result = draftTransactionUseCase.execute(
                walletId = walletId,
                inputs = oldTx.inputs,
                outputs = oldTx.userOutputs.associate { it.first to it.second },
                subtractFeeFromAmount = oldTx.subtractFeeFromAmount,
                feeRate = newFee.toManualFeeRate()
            )) {
                is Result.Success -> _state.value = ConfirmReplaceTransactionState(result.data)
                is Result.Error -> {
                    _event.emit(ReplaceFeeEvent.ShowError(result.exception))
                }
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun replaceTransaction(walletId: String, txId: String, newFee: Int) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))
            val result = replaceTransactionUseCase(ReplaceTransactionUseCase.Data(walletId, txId, newFee))
            _event.emit(ReplaceFeeEvent.Loading(false))
            if (result.isSuccess) {
                _event.emit(ReplaceFeeEvent.ReplaceTransactionSuccess(result.getOrThrow().txId))
            } else {
                _event.emit(ReplaceFeeEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }
}

data class ConfirmReplaceTransactionState(val transaction: Transaction)