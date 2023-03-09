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
import com.nunchuk.android.core.domain.settings.GetQrDensitySettingUseCase
import com.nunchuk.android.core.domain.settings.UpdateQrDensitySettingUseCase
import com.nunchuk.android.core.qr.convertToQRCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.transaction.components.export.ExportTransactionEvent.ExportTransactionError
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.membership.ExportKeystoneDummyTransaction
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class ExportTransactionViewModel @Inject constructor(
    private val exportKeystoneTransactionUseCase: ExportKeystoneTransactionUseCase,
    private val exportKeystoneDummyTransaction: ExportKeystoneDummyTransaction,
    private val getQrDensitySettingUseCase: GetQrDensitySettingUseCase,
    private val updateQrDensitySettingUseCase: UpdateQrDensitySettingUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var args: ExportTransactionArgs

    override val initialState = ExportTransactionState()

    init {
        viewModelScope.launch {
            getQrDensitySettingUseCase(Unit).map { it.getOrThrow() }.collect {
                updateState { copy(density = it) }
                if (this@ExportTransactionViewModel::args.isInitialized) {
                    handleExportTransactionQRs()
                }
            }
        }
    }
    fun init(args: ExportTransactionArgs) {
        this.args = args
        handleExportTransactionQRs()
    }

    fun setQrDensity(density: Int) {
        viewModelScope.launch {
            updateQrDensitySettingUseCase(density)
        }
    }

    private fun handleExportTransactionQRs() {
        if (isDummyTxFlow) exportDummyKeystoneTransaction() else exportTransactionToQRs()
    }

    private fun exportDummyKeystoneTransaction() {
        val qrSize = getQrSize()
        viewModelScope.launch {
            val result = exportKeystoneDummyTransaction(ExportKeystoneDummyTransaction.Param(args.txToSign, getState().density))
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
            exportKeystoneTransactionUseCase.execute(args.walletId, args.txId, getState().density)
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

    private val isDummyTxFlow: Boolean
        get() = args.isDummyTx
}