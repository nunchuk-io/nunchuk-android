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

package com.nunchuk.android.signer

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.BaseNfcUseCase
import com.nunchuk.android.core.domain.GetTapSignerStatusUseCase
import com.nunchuk.android.model.TapSignerStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignerIntroViewModel @Inject constructor(
    private val getTapSignerStatusUseCase: GetTapSignerStatusUseCase
) : ViewModel() {
    private val _event = MutableStateFlow<SignerIntroState?>(null)
    val event = _event.filterIsInstance<SignerIntroState>()

    fun getTapSignerStatus(isoDep: IsoDep?) {
        isoDep ?: return
        _event.value = SignerIntroState.Loading
        viewModelScope.launch {
            val result = getTapSignerStatusUseCase(BaseNfcUseCase.Data(isoDep))
            if (result.isSuccess) {
                Timber.d("TapSigner auth delay ${result.getOrThrow().authDelayInSecond}")
                _event.value = SignerIntroState.GetTapSignerStatusSuccess(result.getOrThrow())
            } else {
                _event.value = SignerIntroState.GetTapSignerStatusError(result.exceptionOrNull())
            }
        }
    }

    fun clearTapSignerStatus() {
        _event.value = null
    }
}

sealed class SignerIntroState {
    object Loading : SignerIntroState()
    class GetTapSignerStatusSuccess(val status: TapSignerStatus) : SignerIntroState()
    class GetTapSignerStatusError(val e: Throwable?) : SignerIntroState()
}