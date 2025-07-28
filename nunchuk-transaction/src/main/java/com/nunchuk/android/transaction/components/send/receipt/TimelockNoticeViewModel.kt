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

package com.nunchuk.android.transaction.components.send.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.usecase.coin.GetAllTagsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimelockNoticeViewModel @Inject constructor(
    private val getAllTagsUseCase: GetAllTagsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelockNoticeUiState())
    val uiState = _uiState.asStateFlow()

    fun init(walletId: String) {
        loadTags(walletId)
    }

    private fun loadTags(walletId: String) {
        viewModelScope.launch {
            getAllTagsUseCase(walletId).onSuccess { tags ->
                _uiState.update { it.copy(tags = tags.associateBy { tag -> tag.id }) }
            }
        }
    }
}

data class TimelockNoticeUiState(
    val tags: Map<Int, CoinTag> = emptyMap(),
)