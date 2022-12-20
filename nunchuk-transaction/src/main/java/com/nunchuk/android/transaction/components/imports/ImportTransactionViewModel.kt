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
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ImportPassportTransactionUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.usecase.membership.GetDummyTxFromPsbt
import com.nunchuk.android.usecase.membership.ParseKeystoneDummyTransaction
import com.nunchuk.android.usecase.membership.ParsePassportDummyTransaction
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ImportTransactionViewModel @Inject constructor(
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val importPassportTransactionUseCase: ImportPassportTransactionUseCase,
    private val parseKeystoneDummyTransaction: ParseKeystoneDummyTransaction,
    private val parsePassportDummyTransaction: ParsePassportDummyTransaction,
    private val getDummyTxFromPsbt: GetDummyTxFromPsbt,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<Unit, ImportTransactionEvent>() {

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
                val psbt = withContext(ioDispatcher) {
                    File(filePath).readText()
                }
                val result = getDummyTxFromPsbt(GetDummyTxFromPsbt.Param(args.walletId, psbt))
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
        if (isDummyFlow) {
            parseDummyTransaction()
        } else {
            parseNormalTransaction()
        }
    }

    private fun parseDummyTransaction() {
        if (isProcessing) return
        viewModelScope.launch {
            isProcessing = true
            val result = if (args.transactionOption == TransactionOption.IMPORT_KEYSTONE) {
                parseKeystoneDummyTransaction(ParseKeystoneDummyTransaction.Param(args.walletId, qrDataList.toList()))
            } else {
                parsePassportDummyTransaction(ParsePassportDummyTransaction.Param(args.walletId, qrDataList.toList()))
            }
            if (result.isSuccess) {
                setEvent(ImportTransactionSuccess(result.getOrThrow()))
            } else {
                setEvent(ImportTransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
            isProcessing = false
        }
    }

    private fun parseNormalTransaction() {
        Timber.d("[ImportTransaction]isProcessing::$isProcessing")
        if (!isProcessing) {
            viewModelScope.launch {
                Timber.d("[ImportTransaction]execute($args.walletId, $qrDataList)")
                if (args.transactionOption == TransactionOption.IMPORT_PASSPORT) {
                    importPassportTransactionUseCase.execute(
                        walletId = args.walletId,
                        qrData = qrDataList.toList(),
                        initEventId = args.initEventId,
                        masterFingerPrint = args.masterFingerPrint
                    )
                } else {
                    importKeystoneTransactionUseCase.execute(
                        walletId = args.walletId,
                        qrData = qrDataList.toList(),
                        initEventId = args.initEventId,
                        masterFingerPrint = args.masterFingerPrint
                    )
                }
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException { setEvent(ImportTransactionError(it.message.orUnknownError())) }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect { event(ImportTransactionSuccess()) }
            }
        }
    }

    private val isDummyFlow: Boolean
        get() = args.isDummyTx
}