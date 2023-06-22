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

package com.nunchuk.android.wallet.components.coin.tagdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.usecase.coin.UpdateCoinTagUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditTagNameBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val updateCoinTagUseCase: UpdateCoinTagUseCase,
    private val assistedWalletManager: AssistedWalletManager,
) : ViewModel() {

    val args = EditTagNameBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<EditTagNameBottomSheetEvent>()
    val event = _event.asSharedFlow()

    private var allTags = arrayListOf<CoinTag>()

    fun onSaveClick(tagName: String) = viewModelScope.launch {
        val existedTag =
            allTags.firstOrNull { it.name == tagName }
        if (existedTag != null) {
            _event.emit(EditTagNameBottomSheetEvent.ExistingTagNameError)
            return@launch
        }
        val coinTag = args.coinTag.copy(name = tagName)
        val result = updateCoinTagUseCase(
            UpdateCoinTagUseCase.Param(
                args.walletId,
                coinTag,
                assistedWalletManager.isActiveAssistedWallet(args.walletId)
            )
        )
        if (result.isSuccess) {
            if (result.getOrDefault(false)) {
                _event.emit(EditTagNameBottomSheetEvent.UpdateTagNameSuccess(tagName = tagName))
            } else {
                _event.emit(EditTagNameBottomSheetEvent.ExistingTagNameError)
            }
        } else {
            _event.emit(EditTagNameBottomSheetEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    fun setTags(tags: List<CoinTag>) {
        allTags.clear()
        allTags.addAll(tags)
    }
}

sealed class EditTagNameBottomSheetEvent {
    data class UpdateTagNameSuccess(val tagName: String) : EditTagNameBottomSheetEvent()
    object ExistingTagNameError : EditTagNameBottomSheetEvent()
    data class Error(val message: String) : EditTagNameBottomSheetEvent()
}