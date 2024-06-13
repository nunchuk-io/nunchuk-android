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

package com.nunchuk.android.core.qr

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetQrDensitySettingUseCase
import com.nunchuk.android.core.domain.settings.UpdateQrDensitySettingUseCase
import com.nunchuk.android.core.util.ExportWalletQRCodeType
import com.nunchuk.android.core.util.HIGH_DENSITY
import com.nunchuk.android.core.util.messageOrUnknownError
import com.nunchuk.android.core.util.toBBQRDensity
import com.nunchuk.android.model.Result
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportBCR2020010WalletUseCase
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.usecase.membership.SaveBitmapToPDFUseCase
import com.nunchuk.android.usecase.qr.ExportBBQRWalletUseCase
import com.nunchuk.android.utils.BitmapUtil
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DynamicQRCodeViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
    private val exportBCR2020010WalletUseCase: ExportBCR2020010WalletUseCase,
    private val getQrDensitySettingUseCase: GetQrDensitySettingUseCase,
    private val updateQrDensitySettingUseCase: UpdateQrDensitySettingUseCase,
    private val exportBBQRWalletUseCase: ExportBBQRWalletUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val saveBitmapToPDFUseCase: SaveBitmapToPDFUseCase,
    ) : ViewModel() {
    val walletId = savedStateHandle.get<String>(DynamicQRCodeArgs.EXTRA_WALLET_ID).orEmpty()
    val type = savedStateHandle.get<Int>(DynamicQRCodeArgs.EXTRA_QR_CODE_TYPE)
        ?: ExportWalletQRCodeType.BC_UR2_LEGACY

    private val _state = MutableStateFlow(DynamicQRCodeState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<DynamicQRCodeEvent>()
    val event = _event.asSharedFlow()

    init {
        viewModelScope.launch {
            getQrDensitySettingUseCase(Unit).map { it.getOrThrow() }
                .distinctUntilChanged()
                .collect { density ->
                _state.update { it.copy(density = density) }
                handleExportWalletQR(density)
            }
        }
        viewModelScope.launch {
            getWalletUseCase.execute(walletId).map { it.wallet.name }.collect { name ->
                _state.update { it.copy(name = name) }
            }
        }
    }

    fun setQrDensity(density: Int) {
        viewModelScope.launch {
            updateQrDensitySettingUseCase(density)
        }
    }

    private fun handleExportWalletQR(density: Int) {
        when (type) {
            ExportWalletQRCodeType.BC_UR2_LEGACY -> {
                viewModelScope.launch {
                    exportKeystoneWalletUseCase.execute(walletId, density)
                        .map { list -> list.mapNotNull { it.convertToQRCode() } }
                        .flowOn(Dispatchers.IO)
                        .onException { }
                        .flowOn(Dispatchers.Main)
                        .collect { bitmaps ->
                            _state.update { it.copy(bitmaps = bitmaps) }
                        }
                }
            }
            ExportWalletQRCodeType.BC_UR2 -> {
                viewModelScope.launch {
                    exportBCR2020010WalletUseCase(
                        ExportBCR2020010WalletUseCase.Params(
                            walletId,
                            density
                        )
                    )
                        .map { list -> list.mapNotNull { it.convertToQRCode() } }
                        .onSuccess { bitmaps ->
                            _state.update { it.copy(bitmaps = bitmaps) }
                        }
                }
            }
            ExportWalletQRCodeType.BBQR -> {
                viewModelScope.launch {
                    val convertDensity = density.toBBQRDensity()
                    exportBBQRWalletUseCase(ExportBBQRWalletUseCase.Params(walletId, convertDensity))
                        .map { list -> list.mapNotNull { it.convertToQRCode() } }
                        .onSuccess { bitmaps ->
                            _state.update { it.copy(bitmaps = bitmaps) }
                        }
                }
            }
        }
    }

    fun saveBitmapToPDF(bitmaps: List<Bitmap>) = viewModelScope.launch(Dispatchers.IO) {
        val pdfName = when (type) {
            ExportWalletQRCodeType.BC_UR2_LEGACY -> "${_state.value.name}_BCUR2_Legacy.pdf"
            ExportWalletQRCodeType.BC_UR2 -> "${_state.value.name}_BCUR2.pdf"
            ExportWalletQRCodeType.BBQR -> "${_state.value.name}_BBQR.pdf"
            else -> ""
        }
        when (val event = createShareFileUseCase.execute(pdfName)) {
            is Result.Success -> {
                saveBitmapToPDFUseCase(
                    SaveBitmapToPDFUseCase.Param(
                        bitmaps,
                        event.data
                    )
                )
                    .onSuccess {
                        _event.emit(DynamicQRCodeEvent.SavePDFSuccess(event.data))
                    }
            }

            is Result.Error -> {
                _event.emit(DynamicQRCodeEvent.Error(event.exception.messageOrUnknownError()))
            }
        }
    }
}

data class DynamicQRCodeState(
    val bitmaps: List<Bitmap> = emptyList(),
    val density: Int = HIGH_DENSITY,
    val name: String = "",
)

sealed class DynamicQRCodeEvent {
    data class SavePDFSuccess(val path: String) : DynamicQRCodeEvent()
    data class Error(val message: String) : DynamicQRCodeEvent()
}