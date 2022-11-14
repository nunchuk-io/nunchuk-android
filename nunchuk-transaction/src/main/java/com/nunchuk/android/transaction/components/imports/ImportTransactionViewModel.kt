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
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionError
import com.nunchuk.android.transaction.components.imports.ImportTransactionEvent.ImportTransactionSuccess
import com.nunchuk.android.usecase.ImportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ImportPassportTransactionUseCase
import com.nunchuk.android.usecase.ImportTransactionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class ImportTransactionViewModel @Inject constructor(
    private val importTransactionUseCase: ImportTransactionUseCase,
    private val importKeystoneTransactionUseCase: ImportKeystoneTransactionUseCase,
    private val importPassportTransactionUseCase: ImportPassportTransactionUseCase
) : NunchukViewModel<Unit, ImportTransactionEvent>() {

    private lateinit var walletId: String
    private lateinit var transactionOption: TransactionOption
    private lateinit var masterFingerPrint: String
    private lateinit var initEventId: String
    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    override val initialState = Unit

    fun init(walletId: String, transactionOption: TransactionOption, masterFingerPrint: String, initEventId: String) {
        this.walletId = walletId
        this.transactionOption = transactionOption
        this.masterFingerPrint = masterFingerPrint
        this.initEventId = initEventId
    }

    fun importTransactionViaFile(filePath: String) {
        viewModelScope.launch {
            importTransactionUseCase.execute(walletId, filePath)
                .flowOn(IO)
                .onException { event(ImportTransactionError(it.readableMessage())) }
                .flowOn(Main)
                .collect { event(ImportTransactionSuccess) }
        }
    }

    fun importTransactionViaQR(qrData: String) {
        qrDataList.add(qrData)
        Timber.d("[ImportTransaction]updateQRCode($qrData)")
        Timber.d("[ImportTransaction]isProcessing::$isProcessing")
        if (!isProcessing) {
            viewModelScope.launch {
                Timber.d("[ImportTransaction]execute($walletId, $qrDataList)")
                if (transactionOption == TransactionOption.IMPORT_PASSPORT) {
                    importPassportTransactionUseCase.execute(walletId = walletId, qrData = qrDataList.toList(), initEventId = initEventId, masterFingerPrint = masterFingerPrint)
                } else {
                    importKeystoneTransactionUseCase.execute(walletId = walletId, qrData = qrDataList.toList(), initEventId = initEventId, masterFingerPrint = masterFingerPrint)
                }
                    .onStart { isProcessing = true }
                    .flowOn(IO)
                    .onException { setEvent(ImportTransactionError(it.message.orUnknownError())) }
                    .flowOn(Main)
                    .onCompletion { isProcessing = false }
                    .collect { event(ImportTransactionSuccess) }
            }
        }
    }

}