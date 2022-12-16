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

package com.nunchuk.android.transaction.components.send.confirmation

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.membership.InheritanceClaimCreateTransactionUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.*
import com.nunchuk.android.usecase.CreateTransactionUseCase
import com.nunchuk.android.usecase.DraftSatsCardTransactionUseCase
import com.nunchuk.android.usecase.DraftTransactionUseCase
import com.nunchuk.android.usecase.room.transaction.InitRoomTransactionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionConfirmViewModel @Inject constructor(
    private val draftTransactionUseCase: DraftTransactionUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase,
    private val initRoomTransactionUseCase: InitRoomTransactionUseCase,
    private val draftSatsCardTransactionUseCase: DraftSatsCardTransactionUseCase,
    private val sessionHolder: SessionHolder,
    private val assistedWalletManager: AssistedWalletManager,
    private val inheritanceClaimCreateTransactionUseCase: InheritanceClaimCreateTransactionUseCase
) : NunchukViewModel<Unit, TransactionConfirmEvent>() {

    private var manualFeeRate: Int = -1
    private lateinit var walletId: String
    private lateinit var address: String
    private var sendAmount: Double = 0.0
    private var subtractFeeFromAmount: Boolean = false
    private val slots = mutableListOf<SatsCardSlot>()
    private lateinit var privateNote: String
    private var masterSignerId: String = ""
    private var magicalPhrase: String = ""

    override val initialState = Unit

    fun init(
        walletId: String,
        address: String,
        sendAmount: Double,
        subtractFeeFromAmount: Boolean,
        privateNote: String,
        manualFeeRate: Int,
        slots: List<SatsCardSlot>,
        masterSignerId: String,
        magicalPhrase: String,
    ) {
        this.walletId = walletId
        this.address = address
        this.sendAmount = sendAmount
        this.subtractFeeFromAmount = subtractFeeFromAmount
        this.privateNote = privateNote
        this.manualFeeRate = manualFeeRate
        this.slots.apply {
            clear()
            addAll(slots)
        }
        this.masterSignerId = masterSignerId
        this.magicalPhrase = magicalPhrase
        draftTransaction()
    }

    private fun initRoomTransaction() {
        event(LoadingEvent)
        viewModelScope.launch {
            val roomId = sessionHolder.getActiveRoomId()
            initRoomTransactionUseCase.execute(
                roomId = roomId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate()
            )
                .flowOn(Dispatchers.IO)
                .onException { event(InitRoomTransactionError(it.message.orUnknownError())) }
                .collect {
                    delay(WAITING_FOR_CONSUME_EVENT_SECONDS)
                    event(InitRoomTransactionSuccess(roomId))
                }
        }
    }

    private fun draftTransaction() {
        if (slots.isEmpty()) {
            draftNormalTransaction()
        } else {
            draftSatsCardTransaction()
        }
    }

    private fun draftNormalTransaction() {
        event(LoadingEvent)
        viewModelScope.launch {
            when (val result = draftTransactionUseCase.execute(
                walletId = walletId,
                outputs = mapOf(address to sendAmount.toAmount()),
                subtractFeeFromAmount = subtractFeeFromAmount,
                feeRate = manualFeeRate.toManualFeeRate()
            )) {
                is Success -> onDraftTransactionSuccess(result.data)
                is Error -> event(CreateTxErrorEvent(result.exception.message.orEmpty()))
            }
        }
    }

    private fun draftSatsCardTransaction() {
        event(LoadingEvent)
        viewModelScope.launch {
            val result = draftSatsCardTransactionUseCase(
                DraftSatsCardTransactionUseCase.Data(
                    address,
                    slots,
                    manualFeeRate
                )
            )
            if (result.isSuccess) {
                onDraftTransactionSuccess(result.getOrThrow())
            } else {
                event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orEmpty()))
            }
        }
    }

    private fun onDraftTransactionSuccess(data: Transaction) {
        val hasChange: Boolean = data.hasChangeIndex()
        if (hasChange) {
            val txOutput = data.outputs[data.changeIndex]
            event(UpdateChangeAddress(txOutput.first, txOutput.second))
        } else {
            event(UpdateChangeAddress("", Amount(0)))
        }
    }

    fun handleConfirmEvent() {
        if (sessionHolder.hasActiveRoom()) {
            initRoomTransaction()
        } else {
            if (isInheritanceClaimingFlow()) {
                createInheritanceTransaction()
            } else {
                createNewTransaction()
            }
        }
    }

    fun isInheritanceClaimingFlow() = masterSignerId.isEmpty() && magicalPhrase.isEmpty()

    private fun createNewTransaction() {
        viewModelScope.launch {
            event(LoadingEvent)
            val result = createTransactionUseCase(
                CreateTransactionUseCase.Param(
                    walletId = walletId,
                    outputs = mapOf(address to sendAmount.toAmount()),
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    feeRate = manualFeeRate.toManualFeeRate(),
                    memo = privateNote,
                    isAssistedWallet = assistedWalletManager.isActiveAssistedWallet(walletId),
                )
            )
            if (result.isSuccess) {
                setEvent(CreateTxSuccessEvent(result.getOrThrow().txId))
            } else {
                event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun createInheritanceTransaction() = viewModelScope.launch {
        event(LoadingEvent)
        val result = inheritanceClaimCreateTransactionUseCase(
            InheritanceClaimCreateTransactionUseCase.Param(
                address = address,
                feeRate = manualFeeRate.toManualFeeRate(),
                masterSignerId = masterSignerId,
                magic = magicalPhrase
            )
        )
        if (result.isSuccess) {
            setEvent(CreateTxSuccessEvent(result.getOrThrow().txId))
        } else {
            event(CreateTxErrorEvent(result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    companion object {
        private const val WAITING_FOR_CONSUME_EVENT_SECONDS = 5L
    }

}

internal fun Int.toManualFeeRate() = if (this > 0) toAmount() else Amount(-1)
