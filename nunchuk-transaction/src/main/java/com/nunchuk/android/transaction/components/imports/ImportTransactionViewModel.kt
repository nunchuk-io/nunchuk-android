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

package com.nunchuk.android.transaction.components.imports

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.membership.ParseKeystoneDummyTransaction
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ImportTransactionViewModel @Inject constructor(
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val parseKeystoneDummyTransaction: ParseKeystoneDummyTransaction,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
) : NunchukViewModel<Unit, ImportTransactionEvent>() {
    private val _state = MutableStateFlow(ImportTransactionState())
    val uiState = _state.asStateFlow()

    private lateinit var args: ImportTransactionArgs
    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun init(args: ImportTransactionArgs) {
        this.args = args
    }

    fun importTransactionViaQR(qrData: String) {
        qrDataList.add(qrData)
        analyzeQr()
        if (isDummyFlow) {
            parseDummyTransaction()
        } else {
            parseNormalTransaction()
        }
    }

    private fun analyzeQr() {
        viewModelScope.launch {
            val result = analyzeQrUseCase(qrDataList.toList())
            if (result.isSuccess) {
                Timber.d("analyzeQrUseCase: ${result.getOrThrow()}")
                _state.update { it.copy(progress = result.getOrThrow().times(100.0)) }
            }
        }
    }

    private fun parseDummyTransaction() {
        if (isProcessing) return
        viewModelScope.launch {
            isProcessing = true
            parseKeystoneDummyTransaction(
                ParseKeystoneDummyTransaction.Param(
                    args.walletId,
                    qrDataList.toList()
                )
            )
                .onSuccess {
                    setEvent(ImportTransactionSuccess(it))
                }.onFailure {
                    Timber.e(it, "[ImportTransaction]")
                }
            isProcessing = false
        }
    }

    private fun parseNormalTransaction() {
        Timber.d("[ImportTransaction]isProcessing::$isProcessing")
        if (!isProcessing) {
            viewModelScope.launch {
                Timber.d("[ImportTransaction]execute($args.walletId, $qrDataList)")
                importKeystoneTransactionUseCase.execute(
                    walletId = args.walletId,
                    qrData = qrDataList.toList(),
                    initEventId = args.initEventId,
                    masterFingerPrint = args.masterFingerPrint
                )
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException {
                        Timber.e(it, "[ImportTransaction]")
                    }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect { event(ImportTransactionSuccess()) }
            }
        }
    }

    private val isDummyFlow: Boolean
        get() = args.isDummyTx
}

data class ImportTransactionState(val progress: Double = 0.0)