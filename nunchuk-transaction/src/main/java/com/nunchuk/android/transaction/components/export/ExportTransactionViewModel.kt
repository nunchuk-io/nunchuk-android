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

package com.nunchuk.android.transaction.components.export

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.share.model.TransactionOption
import com.nunchuk.android.transaction.components.export.ExportTransactionEvent.*
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.ExportPassportTransactionUseCase
import com.nunchuk.android.usecase.ExportTransactionUseCase
import com.nunchuk.android.usecase.membership.ExportKeystoneDummyTransaction
import com.nunchuk.android.usecase.membership.ExportPassportDummyTransaction
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
internal class ExportTransactionViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportTransactionUseCase: ExportTransactionUseCase,
    private val exportPassportTransactionUseCase: ExportPassportTransactionUseCase,
    private val exportKeystoneTransactionUseCase: ExportKeystoneTransactionUseCase,
    private val exportKeystoneDummyTransaction: ExportKeystoneDummyTransaction,
    private val exportPassportDummyTransaction: ExportPassportDummyTransaction,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var args: ExportTransactionArgs

    override val initialState = ExportTransactionState()

    fun init(args: ExportTransactionArgs) {
        this.args = args
        handleExportTransactionQRs()
    }

    private fun handleExportTransactionQRs() {
        if (args.transactionOption == TransactionOption.EXPORT_PASSPORT) {
            if (isDummyTxFlow) exportDummyPassportTransaction() else exportPassportTransaction()
        } else {
            if (isDummyTxFlow) exportDummyKeystoneTransaction() else exportTransactionToQRs()
        }
    }

    fun exportTransactionToFile() {
        viewModelScope.launch {
            event(LoadingEvent)
            when (val result = createShareFileUseCase.execute( if (isDummyTxFlow) "dummy.psbt" else "${args.walletId}_${args.txId}.psbt")) {
                is Success -> exportTransaction(result.data)
                is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
            }
        }
    }

    private fun exportTransaction(filePath: String) {
        viewModelScope.launch {
            if (isDummyTxFlow) {
                val result = runCatching {
                    withContext(ioDispatcher) {
                        FileOutputStream(filePath).use {
                            it.write(args.txToSign.toByteArray(Charsets.UTF_8))
                        }
                    }
                }
                if (result.isSuccess) {
                    setEvent(ExportToFileSuccess(filePath))
                } else {
                    ExportTransactionError(result.exceptionOrNull()?.message.orUnknownError())
                }
            } else {
                when (val result =
                    exportTransactionUseCase.execute(args.walletId, args.txId, filePath)) {
                    is Success -> event(ExportToFileSuccess(filePath))
                    is Error -> event(ExportTransactionError(result.exception.messageOrUnknownError()))
                }
            }
        }
    }

    private fun exportDummyKeystoneTransaction() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            val result = exportKeystoneDummyTransaction(args.txToSign)
            if (result.isSuccess) {
                val bitmaps = withContext(ioDispatcher) {
                    result.getOrThrow()
                        .mapNotNull { it.convertToQRCode(qrSize, qrSize) }
                }
                updateState { copy(qrCodeBitmap = bitmaps) }
            } else {
                setEvent(ExportTransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun exportDummyPassportTransaction() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            val result = exportPassportDummyTransaction(args.txToSign)
            if (result.isSuccess) {
                val bitmaps = withContext(ioDispatcher) {
                    result.getOrThrow()
                        .mapNotNull { it.convertToQRCode(qrSize, qrSize) }
                }
                updateState { copy(qrCodeBitmap = bitmaps) }
            } else {
                setEvent(ExportTransactionError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    private fun exportTransactionToQRs() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            exportKeystoneTransactionUseCase.execute(args.walletId, args.txId)
                .map { it.mapNotNull { qrCode -> qrCode.convertToQRCode(qrSize, qrSize) } }
                .flowOn(IO)
                .onException { event(ExportTransactionError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { updateState { copy(qrCodeBitmap = it) } }
        }
    }

    private fun exportPassportTransaction() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            exportPassportTransactionUseCase.execute(args.walletId, args.txId)
                .map { it.mapNotNull { qrCode -> qrCode.convertToQRCode(qrSize, qrSize) } }
                .flowOn(IO)
                .onException { event(ExportTransactionError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect { updateState { copy(qrCodeBitmap = it) } }
        }
    }

    private fun getQrSize(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    val isDummyTxFlow: Boolean
        get() = args.txToSign.isNotEmpty()
}