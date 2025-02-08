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

package com.nunchuk.android.signer.mk4.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.CreateMk4SignerUseCase
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.usecase.ChangeKeyTypeUseCase
import com.nunchuk.android.usecase.free.groupwallet.AddSignerToGroupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMk4NameViewModel @Inject constructor(
    private val createMk4SignerUseCase: CreateMk4SignerUseCase,
    private val changeKeyTypeUseCase: ChangeKeyTypeUseCase,
    private val pushEventManager: PushEventManager,
    private val addSignerToGroupUseCase: AddSignerToGroupUseCase,
) : ViewModel() {
    private val _event = MutableSharedFlow<AddNameMk4ViewEvent>()
    val event = _event.asSharedFlow()

    fun createMk4Signer(signer: SingleSigner, groupId: String, requestedSignerIndex: Int) {
        viewModelScope.launch {
            _event.emit(AddNameMk4ViewEvent.Loading(true))
            val result = createMk4SignerUseCase(signer)
            _event.emit(AddNameMk4ViewEvent.Loading(false))
            if (result.isSuccess) {
                // for replace key in free wallet
                if (groupId.isNotEmpty()) {
                    addSignerToGroup(signer, groupId, requestedSignerIndex)
                } else {
                    pushEventManager.push(PushEvent.LocalUserSignerAdded(signer))
                    _event.emit(AddNameMk4ViewEvent.CreateMk4SignerSuccess(result.getOrThrow()))
                }
            } else {
                _event.emit(AddNameMk4ViewEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
            }
        }
    }

    fun changeKeyType(signer: SingleSigner, groupId: String, requestedSignerIndex: Int) {
        viewModelScope.launch {
            changeKeyTypeUseCase(
                ChangeKeyTypeUseCase.Params(
                    singleSigner = signer.copy(type = SignerType.COLDCARD_NFC, tags = emptyList())
                )
            ).onSuccess {
                // for replace key in free wallet
                if (groupId.isNotEmpty()) {
                    addSignerToGroup(signer, groupId, requestedSignerIndex)
                } else {
                    pushEventManager.push(PushEvent.LocalUserSignerAdded(signer))
                    _event.emit(AddNameMk4ViewEvent.CreateMk4SignerSuccess(it))
                }
            }.onFailure {
                _event.emit(AddNameMk4ViewEvent.ShowError((it.message.orUnknownError())))
            }
        }
    }

    private suspend fun addSignerToGroup(signer: SingleSigner, groupId: String, requestedSignerIndex: Int) {
        addSignerToGroupUseCase(
            AddSignerToGroupUseCase.Params(
                signer = signer,
                groupId = groupId,
                index = requestedSignerIndex
            )
        ).onSuccess {
            _event.emit(AddNameMk4ViewEvent.CreateMk4SignerSuccess(signer))
        }.onFailure {
            _event.emit(AddNameMk4ViewEvent.ShowError(it.message.orUnknownError()))
        }
    }
}