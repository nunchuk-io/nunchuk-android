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

package com.nunchuk.android.signer.mk4.info

import android.nfc.NdefRecord
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetMk4SingersUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class Mk4InfoViewModel @Inject constructor(
    private val getMk4SingersUseCase: GetMk4SingersUseCase
) : ViewModel() {
    private val _event = MutableSharedFlow<Mk4InfoViewEvent>()
    val event = _event.asSharedFlow()

    private val _mk4Signers = mutableListOf<SingleSigner>()

    val mk4Signers: List<SingleSigner>
        get() = _mk4Signers

    fun getMk4Signer(records: List<NdefRecord>) {
        viewModelScope.launch {
            _event.emit(Mk4InfoViewEvent.Loading(true))
            val result = getMk4SingersUseCase(records.toTypedArray())
            _event.emit(Mk4InfoViewEvent.Loading(false))
            if (result.isSuccess) {
                this@Mk4InfoViewModel._mk4Signers.apply {
                    clear()
                    addAll(result.getOrThrow())
                }
                _event.emit(Mk4InfoViewEvent.LoadMk4SignersSuccess(this@Mk4InfoViewModel._mk4Signers))
            } else {
                _event.emit(Mk4InfoViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }
}