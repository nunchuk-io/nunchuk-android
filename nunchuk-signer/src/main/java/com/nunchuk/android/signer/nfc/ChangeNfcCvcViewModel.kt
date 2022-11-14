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

package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.ChangeCvcTapSignerUseCase
import com.nunchuk.android.core.domain.SetupTapSignerUseCase
import com.nunchuk.android.model.MasterSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChangeNfcCvcViewModel @Inject constructor(
    private val setupTapSignerUseCase: SetupTapSignerUseCase,
    private val changeCvcTapSignerUseCase: ChangeCvcTapSignerUseCase
) : ViewModel() {
    private val _event = MutableStateFlow<ChangeNfcCvcEvent?>(null)
    val event = _event.filterIsInstance<ChangeNfcCvcEvent>()

    fun setUpCvc(isoDep: IsoDep?, oldCvc: String, newCvc: String, chainCode: String) {
        isoDep ?: return
        _event.value = ChangeNfcCvcEvent.Loading
        viewModelScope.launch {
            val result = setupTapSignerUseCase(SetupTapSignerUseCase.Data(isoDep, oldCvc, newCvc, chainCode))
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _event.value = ChangeNfcCvcEvent.SetupCvcSuccess(data.backUpKeyPath, data.masterSigner)
            } else {
                _event.value = ChangeNfcCvcEvent.Error(result.exceptionOrNull())
            }
        }
    }

    fun changeCvc(isoDep: IsoDep?, oldCvc: String, newCvc: String, masterSignerId: String) {
        isoDep ?: return
        _event.value = ChangeNfcCvcEvent.Loading
        viewModelScope.launch {
            val result = changeCvcTapSignerUseCase(ChangeCvcTapSignerUseCase.Data(isoDep, oldCvc, newCvc, masterSignerId))
            if (result.isSuccess && result.getOrThrow()) {
                _event.value = ChangeNfcCvcEvent.ChangeCvcSuccess
            } else {
                _event.value = ChangeNfcCvcEvent.Error(result.exceptionOrNull())
            }
        }
    }

    fun clearEvent() {
        _event.value = null
    }
}

sealed class ChangeNfcCvcEvent {
    object Loading : ChangeNfcCvcEvent()
    object ChangeCvcSuccess : ChangeNfcCvcEvent()
    class SetupCvcSuccess(val backupKeyPath: String, val masterSigner: MasterSigner) : ChangeNfcCvcEvent()
    class Error(val e: Throwable?) : ChangeNfcCvcEvent()
}