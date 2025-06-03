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

package com.nunchuk.android.wallet.personal.components.recover

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ParseQRCodeFromPhotoUseCase
import com.nunchuk.android.core.domain.wallet.ParseKeystoneWalletUseCase
import com.nunchuk.android.usecase.ImportKeystoneWalletUseCase
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class RecoverWalletQrCodeViewModel @Inject constructor(
    private val importKeystoneWalletUseCase: ImportKeystoneWalletUseCase,
    private val parseKeystoneWalletUseCase: ParseKeystoneWalletUseCase,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
    private val parseQRCodeFromPhotoUseCase: ParseQRCodeFromPhotoUseCase
) : ViewModel() {

    private var isProcessing = false

    private val qrDataList = HashSet<String>()

    private val _state = MutableStateFlow(RecoverWalletQrCodeUiState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<RecoverWalletQrCodeEvent>()
    val event = _event.asSharedFlow()

    fun updateQRCode(isParseOnly: Boolean, qrData: String, description: String) {
        qrDataList.add(qrData)
        viewModelScope.launch {
            analyzeQrUseCase(qrDataList.toList()).onSuccess { value ->
                val progress = value.times(100.0)
                val isSingleQR = qrDataList.size == 1 && value == 0.0
                val isDynamicQR = progress >= 100
                _state.update { it.copy(progress = progress) }
                if (!isProcessing && (isDynamicQR || isSingleQR)) {
                    isProcessing = true
                    if (isParseOnly) {
                        parseKeystoneWalletUseCase(qrDataList.toList())
                            .onSuccess {
                                _event.emit(RecoverWalletQrCodeEvent.ImportQRCodeSuccess(it))
                            }
                            .onFailure {
                                Timber.tag("recover-wallet").e("Parse QR Code Error: ${it.message}")
                                isProcessing = false
                            }
                    } else {
                        importKeystoneWalletUseCase.execute(
                            description = description,
                            qrData = qrDataList.toList()
                        )
                            .flowOn(IO)
                            .onException {
                                isProcessing = false
                                Timber.tag("recover-wallet")
                                    .e("Import QR Code Error: ${it.message}")
                                _event.emit(RecoverWalletQrCodeEvent.ImportQRCodeError(it.message.orEmpty()))
                            }
                            .flowOn(Main)
                            .collect {
                                Timber.tag("recover-wallet").e("Import QR Code Success")
                                _event.emit(RecoverWalletQrCodeEvent.ImportQRCodeSuccess(it))
                            }
                    }
                }
            }.onFailure {
                Timber.tag("recover-wallet").e("Analyze QR Error: ${it.message}")
            }
        }
    }

    fun decodeQRCodeFromUri(uri: Uri) {
        viewModelScope.launch {
            parseQRCodeFromPhotoUseCase(uri).onSuccess {
                _event.emit(RecoverWalletQrCodeEvent.ParseQRCodeFromPhotoSuccess(it))
            }.onFailure {
                _event.emit(RecoverWalletQrCodeEvent.ImportQRCodeError(it.message.orEmpty()))
            }
        }
    }

    fun getQrList(): List<String> {
        return qrDataList.toList()
    }
}

data class RecoverWalletQrCodeUiState(
    val progress: Double = 0.0,
)