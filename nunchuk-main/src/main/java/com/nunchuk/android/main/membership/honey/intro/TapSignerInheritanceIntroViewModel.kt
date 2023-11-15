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

package com.nunchuk.android.main.membership.honey.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.mapper.MasterSignerMapper
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TapSignerInheritanceIntroViewModel @Inject constructor(
    private val membershipStepManager: MembershipStepManager,
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val masterSignerMapper: MasterSignerMapper,
) : ViewModel() {
    private val _event = MutableSharedFlow<TapSignerInheritanceIntroEvent>()
    val event = _event.asSharedFlow()

    private val _tapSigners = MutableStateFlow<List<SignerModel>>(emptyList())
    val tapSigners = _tapSigners.asStateFlow()

    init {
        viewModelScope.launch {
            getMasterSignersUseCase.execute()
                .collect {
                    val signers = it.map { signer -> masterSignerMapper(signer) }
                        .filter { signer ->
                            signer.type == SignerType.NFC && membershipStepManager.isKeyExisted(
                                signer.fingerPrint
                            ).not()
                        }

                    _tapSigners.update { signers }
                }
        }
    }

    fun getTapSigners() = tapSigners.value

    fun onContinueClicked() {
        viewModelScope.launch {
            _event.emit(TapSignerInheritanceIntroEvent.OnContinueClicked)
        }
    }

    val remainTime = membershipStepManager.remainingTime
}

sealed class TapSignerInheritanceIntroEvent {
    object OnContinueClicked : TapSignerInheritanceIntroEvent()
}