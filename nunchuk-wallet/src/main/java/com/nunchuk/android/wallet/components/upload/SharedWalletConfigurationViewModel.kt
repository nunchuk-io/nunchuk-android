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

package com.nunchuk.android.wallet.components.upload

import android.nfc.tech.Ndef
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ExportWalletToMk4UseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.Result.Error
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.type.ExportFormat
import com.nunchuk.android.usecase.CreateShareFileUseCase
import com.nunchuk.android.usecase.ExportWalletUseCase
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.DoneScanQr
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.ExportColdcardSuccess
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.NfcLoading
import com.nunchuk.android.wallet.components.upload.UploadConfigurationEvent.ShowError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWalletConfigurationViewModel @Inject constructor(
    private val createShareFileUseCase: CreateShareFileUseCase,
    private val exportWalletUseCase: ExportWalletUseCase,
    private val exportWalletToMk4UseCase: ExportWalletToMk4UseCase,
) : ViewModel() {

    lateinit var walletId: String

    private val _event = MutableSharedFlow<UploadConfigurationEvent>()
    val event = _event.asSharedFlow()

    fun init(walletId: String) {
        this.walletId = walletId
    }

    fun handleColdcardExportNfc(ndef: Ndef) {
        viewModelScope.launch {
            _event.emit(NfcLoading(true))
            val result = exportWalletToMk4UseCase(ExportWalletToMk4UseCase.Data(walletId, ndef))
            _event.emit(NfcLoading(false))
            if (result.isSuccess) {
                _event.emit(ExportColdcardSuccess())
            } else {
                _event.emit(ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun handleColdcardExportToFile() {
        viewModelScope.launch {
            when (val event = createShareFileUseCase.execute(walletId + "_coldcard_export.txt")) {
                is Success -> exportWallet(walletId, event.data)
                is Error -> {
                    _event.emit(ShowError(event.exception.message.orUnknownError()))
                }
            }
        }
    }

    fun doneScanQr() {
        viewModelScope.launch {
            _event.emit(DoneScanQr)
        }
    }

    private fun exportWallet(walletId: String, filePath: String) {
        viewModelScope.launch {
            when (val event = exportWalletUseCase.execute(walletId, filePath, ExportFormat.COLDCARD)) {
                is Success -> {
                    _event.emit(ExportColdcardSuccess(filePath))
                }
                is Error -> showError(event.exception)
            }
        }
    }

    private fun showError(t: Throwable?) {
        viewModelScope.launch {
            _event.emit(ShowError(t?.message.orUnknownError()))
        }
    }
}