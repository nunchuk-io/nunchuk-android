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

package com.nunchuk.android.transaction.components.send.batchtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.fromBTCtoSAT
import com.nunchuk.android.core.util.fromCurrencyToBTC
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.core.util.getBTCAmountWithoutSat
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.transaction.components.utils.privateNote
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.coin.GetAllCoinUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import com.nunchuk.android.usecase.wallet.GetWalletDetail2UseCase
import com.nunchuk.android.utils.toSafeDoubleAmount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BatchTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val getAllCoinUseCase: GetAllCoinUseCase,
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val getWalletDetail2UseCase: GetWalletDetail2UseCase
) : ViewModel() {

    private val args = BatchTransactionFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<BatchTransactionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(BatchTransactionState())
    val state = _state.asStateFlow()

    private var availableAmountWithoutUnlocked: Double = 0.0
    private var hasLockedCoin: Boolean = false
    private var isMiniscript: Boolean = false

    init {
        if (!args.isFromSelectCoin) {
            checkLockedCoin(args.walletId)
        }
        viewModelScope.launch {
            getWalletDetail2UseCase(args.walletId)
                .onSuccess {
                    isMiniscript = it.miniscript.isNotEmpty()
                }
        }
    }

    private fun checkLockedCoin(walletId: String) {
        viewModelScope.launch {
            _event.emit(BatchTransactionEvent.Loading(true))
            getAllCoinUseCase(walletId).onSuccess {
                hasLockedCoin = it.any { coin -> coin.isLocked }
                availableAmountWithoutUnlocked =
                    it.sumOf { coin -> if (coin.isLocked) 0.0 else coin.amount.pureBTC() }
            }
            _event.emit(BatchTransactionEvent.Loading(false))
        }
    }

    fun isEnableCreateTransaction(): Boolean {
        return _state.value.recipients.any { it.amount.isEmpty() || it.address.isEmpty() }.not()
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                if (btcUri.amount.value > 0) {
                    updateRecipient(
                        index = state.value.interactingIndex,
                        amount = btcUri.amount.pureBTC().toString(),
                        address = btcUri.address,
                        isBtc = true,
                        selectAddressType = SelectAddressType.NONE.ordinal,
                        invalidAddress = false
                    )
                    updateNoteChange(btcUri.privateNote)
                } else {
                    updateRecipient(
                        index = state.value.interactingIndex,
                        address = btcUri.address,
                        selectAddressType = SelectAddressType.NONE.ordinal,
                        invalidAddress = false
                    )
                }
            } else {
                _event.emit(BatchTransactionEvent.Error(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun createTransaction(isCustomTx: Boolean, availableAmount: Double, subtractFeeFromAmount: Boolean) = viewModelScope.launch {
        val amount = getTotalAmount()
        if (amount <= 0 || amount > availableAmount) {
            _event.emit(BatchTransactionEvent.InsufficientFundsEvent)
        } else if (amount > availableAmountWithoutUnlocked && !args.isFromSelectCoin) {
            _event.emit(BatchTransactionEvent.InsufficientFundsLockedCoinEvent)
        } else {
            var isAllValidAddress = true
            _state.value.recipients.forEach { recipient ->
                checkAddressValidUseCase(CheckAddressValidUseCase.Params(arrayListOf(recipient.address)))
                    .onSuccess {
                        if (it.isNotEmpty()) {
                            isAllValidAddress = false
                            updateRecipient(
                                index = state.value.recipients.indexOf(recipient),
                                invalidAddress = true
                            )
                        }
                    }
            }
            if (isAllValidAddress) {
                _state.update { it.copy(subtractFeeFromAmount = subtractFeeFromAmount) }
                _event.emit(BatchTransactionEvent.CheckAddressSuccess(
                    isCustomTx = isCustomTx,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    isMiniscript = isMiniscript
                ))
            }
        }
    }

    fun getFirstUnusedAddress(walletId: String, walletName: String) {
        viewModelScope.launch {
            getUnusedWalletAddressUseCase(walletId).onSuccess { addresses ->
                updateRecipient(
                    index = state.value.interactingIndex,
                    address = addresses.first(),
                    selectAddressType = SelectAddressType.WALLET.ordinal,
                    selectAddressName = walletName,
                    walletId = walletId
                )
            }.onFailure {
                _event.emit(BatchTransactionEvent.Error(it.message.orUnknownError()))
            }
        }
    }

    private fun getTotalAmount() = _state.value.recipients.sumOf {
        if (it.isBtc) {
            if (CURRENT_DISPLAY_UNIT_TYPE == SAT) it.amount.toSafeDoubleAmount()
                .fromSATtoBTC() else it.amount.toSafeDoubleAmount()
        } else {
            it.amount.toSafeDoubleAmount().fromCurrencyToBTC()
        }
    }

    fun updateRecipient(
        index: Int,
        amount: String? = null,
        isBtc: Boolean? = null,
        address: String? = null,
        selectAddressType: Int? = null,
        selectAddressName: String? = null,
        invalidAddress: Boolean? = null,
        walletId: String? = null
    ) {
        var recipient = _state.value.recipients[index]
        amount?.let {
            recipient = recipient.copy(amount = it)
        }
        isBtc?.let {
            recipient = recipient.copy(isBtc = it)
            val recipients = _state.value.recipients.map {  recipient ->
                recipient.copy(isBtc = it)
            }.toMutableList()
            _state.update { it.copy(recipients = recipients) }
        }
        address?.let {
            recipient = recipient.copy(address = it)
        }
        selectAddressType?.let {
            recipient = recipient.copy(selectAddressType = it)
            if (it == SelectAddressType.ADDRESS.ordinal ||  it == SelectAddressType.NONE.ordinal) {
                recipient = recipient.copy(walletId = "")
            }
            if (it == SelectAddressType.NONE.ordinal) {
                recipient = recipient.copy(selectAddressName = "")
            }
        }
        selectAddressName?.let {
            recipient = recipient.copy(selectAddressName = it)
        }
        invalidAddress?.let {
            recipient = recipient.copy(invalidAddress = it)
        }
        walletId?.let {
            recipient = recipient.copy(walletId = it)
        }
        val newRecipients = _state.value.recipients.toMutableList()
        newRecipients[index] = recipient
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun getTxReceiptList() = _state.value.recipients.map {
        TxReceipt(
            address = it.address, amount = if (it.isBtc) {
                if (CURRENT_DISPLAY_UNIT_TYPE == SAT) it.amount.toSafeDoubleAmount()
                    .fromSATtoBTC() else it.amount.toSafeDoubleAmount()
            } else {
                it.amount.toSafeDoubleAmount().fromCurrencyToBTC()
            }
        )
    }

    fun getNote() = _state.value.note

    fun getSubtractFeeFromAmount() = _state.value.subtractFeeFromAmount

    fun addRecipient() {
        val newRecipients = _state.value.recipients.toMutableList()
        newRecipients.add(BatchTransactionState.Recipient.DEFAULT)
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun isEnableRemoveRecipient() = _state.value.recipients.toMutableList().size > 1

    fun removeRecipient(index: Int) {
        val newRecipients = _state.value.recipients.toMutableList()
        if (newRecipients.size == 1) return
        newRecipients.removeAt(index)
        _state.update { it.copy(recipients = newRecipients) }
    }

    fun updateNoteChange(note: String) {
        _state.update { it.copy(note = note) }
    }

    fun setInteractingIndex(index: Int) {
        _state.update { it.copy(interactingIndex = index) }
    }

    fun getInteractingIndex() = _state.value.interactingIndex

    fun getRecipients() = _state.value.recipients

    fun sendAllRemaining(availableAmount: Double, index: Int) = viewModelScope.launch {
        val remainingAmount = availableAmount - getTotalAmount()
        if (remainingAmount <= 0) return@launch
        if (remainingAmount > availableAmountWithoutUnlocked && !args.isFromSelectCoin) {
            _event.emit(BatchTransactionEvent.InsufficientFundsLockedCoinEvent)
            return@launch
        }
        val recipient = _state.value.recipients[index]
        val isBtc = recipient.isBtc
        val amount = if (isBtc) {
            if (CURRENT_DISPLAY_UNIT_TYPE == SAT) remainingAmount.fromBTCtoSAT()
            else remainingAmount.getBTCAmountWithoutSat()
        } else {
            remainingAmount.fromBTCToCurrency()
        }
        updateRecipient(index = index, amount = amount.toString())
    }
}