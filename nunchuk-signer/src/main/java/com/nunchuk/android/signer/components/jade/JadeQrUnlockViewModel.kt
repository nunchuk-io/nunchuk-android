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
package com.nunchuk.android.signer.components.jade

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.nativeErrorCode
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.usecase.qr.AnalyzeQrUseCase
import com.nunchuk.android.usecase.qr.HandleJadePinQrUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Collects the QR fragments a locked Jade shows and resolves them against the Blockstream PIN
 * server. Rendering the reply is left to the shared dynamic-QR screen, which already has the
 * density control the fragment length needs.
 */
@HiltViewModel
class JadeQrUnlockViewModel @Inject constructor(
    private val handleJadePinQrUseCase: HandleJadePinQrUseCase,
    private val analyzeQrUseCase: AnalyzeQrUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(JadeQrUnlockState())
    val state = _state.asStateFlow()

    private val _event = MutableSharedFlow<JadeQrUnlockEvent>()
    val event = _event.asSharedFlow()

    private val qrDataList = LinkedHashSet<String>()
    private var isProcessing = false

    fun onQrScanned(qrData: String) {
        if (!qrDataList.add(qrData) || isProcessing) return
        isProcessing = true
        viewModelScope.launch {
            try {
                analyzeQr()
                resolvePin()
            } finally {
                isProcessing = false
            }
        }
    }

    private suspend fun analyzeQr() {
        analyzeQrUseCase(qrDataList.toList()).onSuccess { percent ->
            _state.update { it.copy(progress = percent.times(100.0)) }
        }
    }

    private suspend fun resolvePin() {
        _event.emit(JadeQrUnlockEvent.Loading(true))
        handleJadePinQrUseCase(qrDataList.toList())
            .onSuccess { pin ->
                Timber.tag(TAG).d("Jade PIN request resolved, %d bytes", pin.length)
                _event.emit(JadeQrUnlockEvent.ShowReply(pin))
            }
            .onFailure { throwable ->
                // Every fragment of a multi-part QR fails until the set is complete, so stay
                // quiet until there is nothing left to scan.
                if (_state.value.progress >= 100) reportFailure(throwable)
            }
        _event.emit(JadeQrUnlockEvent.Loading(false))
    }

    private suspend fun reportFailure(throwable: Throwable) {
        qrDataList.clear()
        _state.update { it.copy(progress = 0.0) }
        _event.emit(
            JadeQrUnlockEvent.Error(throwable.message.orUnknownError(), throwable.nativeErrorCode())
        )
    }

    /** Returns to scanning for the next leg of the handshake. */
    fun reset() {
        qrDataList.clear()
        _state.update { JadeQrUnlockState() }
    }

    companion object {
        private const val TAG = "JadeQrUnlock"
    }
}

data class JadeQrUnlockState(val progress: Double = 0.0)
