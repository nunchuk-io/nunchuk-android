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
import com.nunchuk.android.ReplaceTransactionUseCase
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxInput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.components.send.confirmation.toManualFeeRate
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import com.nunchuk.android.usecase.coin.GetCoinsFromTxInputsUseCase
import com.nunchuk.android.usecase.membership.GetSavedAddressListLocalUseCase
import com.nunchuk.android.usecase.membership.ReplaceServerTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfirmReplaceTransactionViewModel @Inject constructor(
    private val replaceTransactionUseCase: ReplaceTransactionUseCase,
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val getCoinsFromTxInputsUseCase: GetCoinsFromTxInputsUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    private val replaceServerTransactionUseCase: ReplaceServerTransactionUseCase,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getSavedAddressListLocalUseCase: GetSavedAddressListLocalUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<ReplaceFeeEvent>()
    private val _state = MutableStateFlow(ConfirmReplaceTransactionState())
    val state = _state.asStateFlow()
    val event = _event.asSharedFlow()

    private var antiFeeSniping: Boolean = false

    init {
        viewModelScope.launch {
            getSavedAddressListLocalUseCase(Unit)
                .map { it.getOrThrow() }
                .collect { savedAddresses ->
                    _state.update { it.copy(savedAddress = savedAddresses.associate { address -> address.address to address.label }) }
                }
        }
    }

    fun init(walletId: String, oldTx: Transaction, antiFeeSniping: Boolean) {
        this.antiFeeSniping = antiFeeSniping
        viewModelScope.launch {
            getCoinsFromTxInputsUseCase(
                GetCoinsFromTxInputsUseCase.Params(
                    walletId = walletId,
                    txInputs = oldTx.inputs
                )
            ).onSuccess { coins ->
                _state.update { it.copy(inputCoins = coins, transaction = oldTx) }
            }.onFailure {
                _event.emit(ReplaceFeeEvent.ShowError(it))
            }
        }
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _state.update { it.copy(allTags = tags.associateBy { tag -> tag.id }) }
            }
        }
    }

    fun draftTransaction(walletId: String, oldTx: Transaction, newFee: Int) {
        viewModelScope.launch {
            delay(150) // work around shared flow not show loading
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
                _state.update { it.copy(transaction = transaction) }
            }.onFailure {
                _event.emit(ReplaceFeeEvent.ShowError(it))
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun draftCancelTransaction(walletId: String, oldTx: Transaction, newFee: Int, address: String) {
        viewModelScope.launch {
            delay(150) // work around shared flow not show loading
            _event.emit(ReplaceFeeEvent.Loading(true))
            getCoinsFromTxInputsUseCase(
                GetCoinsFromTxInputsUseCase.Params(
                    walletId = walletId,
                    txInputs = oldTx.inputs
                )
            ).onSuccess { coins ->
                draftTransactionUseCase(
                    DraftTransactionUseCase.Params(
                        walletId = walletId,
                        inputs = coins.map { TxInput(it.txid, it.vout) },
                        outputs = mapOf(address to coins.sumOf { it.amount.value }.toAmount()),
                        subtractFeeFromAmount = true,
                        feeRate = newFee.toManualFeeRate(),
                        replaceTxId = oldTx.txId
                    )
                ).onSuccess { tx ->
                    _state.update { it.copy(transaction = tx) }
                }.onFailure {
                    _event.emit(ReplaceFeeEvent.ShowError(it))
                }
            }.onFailure {
                _event.emit(ReplaceFeeEvent.ShowError(it))
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun replaceTransaction(walletId: String, txId: String, newFee: Int) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))
            val result = replaceTransactionUseCase(
                ReplaceTransactionUseCase.Data(
                    groupId = assistedWalletManager.getGroupId(walletId),
                    walletId = walletId,
                    txId = txId,
                    newFee = newFee,
                    antiFeeSniping = antiFeeSniping
                )
            )
            _event.emit(ReplaceFeeEvent.Loading(false))
            if (result.isSuccess) {
                if (assistedWalletManager.isActiveAssistedWallet(walletId)) {
                    replaceServerTransactionUseCase(
                        ReplaceServerTransactionUseCase.Params(
                            groupId = assistedWalletManager.getGroupId(walletId),
                            walletId = walletId,
                            transactionId = txId,
                            newTxPsbt = result.getOrThrow().psbt
                        )
                    )
                }
                _event.emit(ReplaceFeeEvent.ReplaceTransactionSuccess(result.getOrThrow().txId))
            } else {
                _event.emit(ReplaceFeeEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun createTransaction(walletId: String, oldTx: Transaction, newFee: Int, address: String) {
        viewModelScope.launch {
            _event.emit(ReplaceFeeEvent.Loading(true))
            getCoinsFromTxInputsUseCase(
                GetCoinsFromTxInputsUseCase.Params(
                    walletId = walletId,
                    txInputs = oldTx.inputs
                )
            ).onSuccess { coins ->
                createTransactionUseCase(
                    CreateTransactionUseCase.Param(
                        groupId = assistedWalletManager.getGroupId(walletId),
                        walletId = walletId,
                        outputs = mapOf(address to coins.sumOf { it.amount.value }.toAmount()),
                        inputs = coins.map { TxInput(it.txid, it.vout) },
                        feeRate = newFee.toManualFeeRate(),
                        subtractFeeFromAmount = true,
                        isAssistedWallet = false, // hard code to false to don't sync to server
                        replaceTxId = oldTx.txId,
                        antiFeeSniping = antiFeeSniping
                    )
                ).onSuccess {
                    if (assistedWalletManager.isActiveAssistedWallet(walletId)) {
                        replaceServerTransactionUseCase(
                            ReplaceServerTransactionUseCase.Params(
                                groupId = assistedWalletManager.getGroupId(walletId),
                                walletId = walletId,
                                transactionId = oldTx.txId,
                                newTxPsbt = it.psbt
                            )
                        )
                    }
                    _event.emit(ReplaceFeeEvent.ReplaceTransactionSuccess(it.txId))
                }.onFailure {
                    _event.emit(ReplaceFeeEvent.ShowError(it))
                }
            }.onFailure {
                _event.emit(ReplaceFeeEvent.ShowError(it))
            }
            _event.emit(ReplaceFeeEvent.Loading(false))
        }
    }

    fun getSavedAddress(): Map<String, String> {
        return _state.value.savedAddress
    }
}

data class ConfirmReplaceTransactionState(
    val transaction: Transaction? = null,
    val inputCoins: List<UnspentOutput> = emptyList(),
    val savedAddress: Map<String, String> = emptyMap(),
    val allTags: Map<Int, CoinTag> = emptyMap()
)