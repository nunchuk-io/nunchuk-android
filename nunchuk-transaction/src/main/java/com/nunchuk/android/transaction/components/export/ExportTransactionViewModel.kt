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

package com.nunchuk.android.transaction.components.export

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.settings.GetQrDensitySettingUseCase
import com.nunchuk.android.core.domain.settings.UpdateQrDensitySettingUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.core.util.toBBQRDensity
import com.nunchuk.android.transaction.components.export.ExportTransactionEvent.ExportTransactionError
import com.nunchuk.android.usecase.ExportKeystoneTransactionUseCase
import com.nunchuk.android.usecase.membership.ExportKeystoneDummyTransaction
import com.nunchuk.android.usecase.qr.ExportBBQRTransactionUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
internal class ExportTransactionViewModel @Inject constructor(
    private val exportKeystoneTransactionUseCase: ExportKeystoneTransactionUseCase,
    private val exportKeystoneDummyTransaction: ExportKeystoneDummyTransaction,
    private val getQrDensitySettingUseCase: GetQrDensitySettingUseCase,
    private val updateQrDensitySettingUseCase: UpdateQrDensitySettingUseCase,
    private val exportBBQRTransactionUseCase: ExportBBQRTransactionUseCase,
) : NunchukViewModel<ExportTransactionState, ExportTransactionEvent>() {

    private lateinit var args: ExportTransactionArgs

    override val initialState = ExportTransactionState()

    private var exportTransactionJob: Job? = null

    init {
        viewModelScope.launch {
            getQrDensitySettingUseCase(Unit).map { it.getOrThrow() }
                .distinctUntilChanged()
                .collect {
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
        if (isDummyTxFlow) {
            exportDummyKeystoneTransaction()
        } else if (args.isBBQR) {
            exportTransactionBBQR()
        } else {
            exportTransactionToQRs()
        }
    }

    private fun exportTransactionBBQR() {
        viewModelScope.launch {
            val convertDensity = getState().density.toBBQRDensity()
            exportBBQRTransactionUseCase(
                ExportBBQRTransactionUseCase.Params(
                    walletId = args.walletId,
                    txId = args.txId,
                    density = convertDensity
                )
            ).onSuccess {
                updateState {
                    copy(qrStrings = it)
                }
            }.onFailure {
                if (it !is CancellationException) {
                    setEvent(ExportTransactionError(it.message.orUnknownError()))
                }
            }
        }
    }

    private fun exportDummyKeystoneTransaction() {
        exportTransactionJob?.cancel()
        exportTransactionJob = viewModelScope.launch {
            exportKeystoneDummyTransaction(
                ExportKeystoneDummyTransaction.Param(
                    txToSign = args.txToSign,
                    density = if (args.isBBQR) getState().density.toBBQRDensity() else getState().density,
                    isBBQR = args.isBBQR
                )
            ).onSuccess {
                updateState {
                    copy(qrStrings = it)
                }
            }.onFailure {
                if (it !is CancellationException) {
                    setEvent(ExportTransactionError(it.message.orUnknownError()))
                }
            }
        }
    }

    private fun exportTransactionToQRs() {
        exportTransactionJob?.cancel()
        exportTransactionJob = viewModelScope.launch {
            exportKeystoneTransactionUseCase.execute(args.walletId, args.txId, getState().density)
                .flowOn(IO)
                .onException { event(ExportTransactionError(it.message.orUnknownError())) }
                .flowOn(Main)
                .collect {
                    updateState { copy(qrStrings = it) }
                }
        }
    }

    val qrStrings: List<String>
        get() = getState().qrStrings

    private val isDummyTxFlow: Boolean
        get() = args.isDummyTx
}