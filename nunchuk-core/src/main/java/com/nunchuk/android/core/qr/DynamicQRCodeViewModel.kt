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

package com.nunchuk.android.core.qr

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetQrDensitySettingUseCase
import com.nunchuk.android.core.domain.settings.UpdateQrDensitySettingUseCase
import com.nunchuk.android.core.util.HIGH_DENSITY
import com.nunchuk.android.usecase.ExportKeystoneWalletUseCase
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DynamicQRCodeViewModel @Inject constructor(
    private val getWalletUseCase: GetWalletUseCase,
    private val exportKeystoneWalletUseCase: ExportKeystoneWalletUseCase,
    private val getQrDensitySettingUseCase: GetQrDensitySettingUseCase,
    private val updateQrDensitySettingUseCase: UpdateQrDensitySettingUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val walletId = savedStateHandle.get<String>(DynamicQRCodeArgs.EXTRA_WALLET_ID).orEmpty()

    private val _state = MutableStateFlow(DynamicQRCodeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getQrDensitySettingUseCase(Unit).map { it.getOrThrow() }.collect { density ->
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
}

data class DynamicQRCodeState(val bitmaps: List<Bitmap> = emptyList(), val density: Int = HIGH_DENSITY, val name: String = "")