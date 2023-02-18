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

package com.nunchuk.android.transaction.components.imports

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.readableMessage
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.membership.GetDummyTxFromPsbtByteArrayUseCase
import com.nunchuk.android.usecase.membership.ParseKeystoneDummyTransaction
import com.nunchuk.android.usecase.membership.ParsePassportDummyTransaction
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ImportTransactionViewModel @Inject constructor(
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val parseKeystoneDummyTransaction: ParseKeystoneDummyTransaction,
    private val getDummyTxFromPsbtByteArrayUseCase: GetDummyTxFromPsbtByteArrayUseCase,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

    fun importTransactionViaFile(filePath: String) {
        viewModelScope.launch {
            if (isDummyFlow) {
                val bytes = withContext(ioDispatcher) {
                    File(filePath).readBytes()
                }
                val result = getDummyTxFromPsbtByteArrayUseCase(GetDummyTxFromPsbtByteArrayUseCase.Param(args.walletId, bytes))
                if (result.isSuccess) {
                    setEvent(ImportTransactionSuccess(result.getOrThrow()))
                } else {
                    setEvent(ImportTransactionError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            } else {
                importTransactionUseCase.execute(args.walletId, filePath)
                    .flowOn(IO)
                    .onException { event(ImportTransactionError(it.readableMessage())) }
                    .flowOn(Main)
                    .collect { event(ImportTransactionSuccess()) }
            }
        }
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
            val result = parseKeystoneDummyTransaction(ParseKeystoneDummyTransaction.Param(args.walletId, qrDataList.toList()))
            if (result.isSuccess) {
                setEvent(ImportTransactionSuccess(result.getOrThrow()))
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
                    .onException {  }
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