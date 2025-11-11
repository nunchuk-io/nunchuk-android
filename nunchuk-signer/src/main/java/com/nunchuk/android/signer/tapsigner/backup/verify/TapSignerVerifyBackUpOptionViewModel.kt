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

package com.nunchuk.android.signer.tapsigner.backup.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.usecase.membership.SetKeyVerifiedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerVerifyBackUpOptionViewModel @Inject constructor(
    private val setKeyVerifiedUseCase: SetKeyVerifiedUseCase
) : ViewModel() {

    private val _event = MutableSharedFlow<TapSignerVerifyBackUpOptionEvent>()
    val event = _event.asSharedFlow()

    fun skipVerification(groupId: String, masterSignerId: String) {
        if (masterSignerId.isEmpty()) {
            viewModelScope.launch {
                _event.emit(TapSignerVerifyBackUpOptionEvent.SkipVerificationError(Exception("Missing masterSignerId")))
            }
            return
        }

        viewModelScope.launch {
            val result = setKeyVerifiedUseCase(
                SetKeyVerifiedUseCase.Param(
                    groupId = groupId,
                    masterSignerId = masterSignerId,
                    verifyType = VerifyType.SKIPPED_VERIFICATION
                )
            )
            if (result.isSuccess) {
                _event.emit(TapSignerVerifyBackUpOptionEvent.SkipVerificationSuccess)
            } else {
                _event.emit(TapSignerVerifyBackUpOptionEvent.SkipVerificationError(result.exceptionOrNull()))
            }
        }
    }
}

sealed class TapSignerVerifyBackUpOptionEvent {
    data object SkipVerificationSuccess : TapSignerVerifyBackUpOptionEvent()
    data class SkipVerificationError(val error: Throwable?) : TapSignerVerifyBackUpOptionEvent()
}

