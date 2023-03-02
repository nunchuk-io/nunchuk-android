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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.bufferperiod

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.GetInheritanceBufferPeriodUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Period
import com.nunchuk.android.share.membership.MembershipStepManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InheritanceBufferPeriodViewModel @Inject constructor(
    private val getInheritanceBufferPeriodUseCase: GetInheritanceBufferPeriodUseCase,
    @ApplicationContext private val context: Context,
    membershipStepManager: MembershipStepManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val args = InheritanceBufferPeriodFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _event = MutableSharedFlow<InheritanceBufferPeriodEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(InheritanceBufferPeriodState())
    val state = _state.asStateFlow()

    val remainTime = membershipStepManager.remainingTime

    init {
        getBufferPeriod()
    }

    private fun getBufferPeriod() = viewModelScope.launch {
        val result = getInheritanceBufferPeriodUseCase(Unit)
        if (result.isSuccess) {
            val options = result.getOrNull()?.map {
                BufferPeriodOption(it, isSelected = it.isRecommended)
            }.orEmpty().toMutableList()
            options.add(getNoNeedPeriodItem())
            _state.update {
                it.copy(options = options)
            }
            if (args.isUpdateRequest) {
                onOptionClick(args.preBufferPeriod?.id ?: NO_NEED_PERIOD_ITEM_ID)
            }
        } else {
            _event.emit(InheritanceBufferPeriodEvent.Error(message = result.exceptionOrNull()?.message.orUnknownError()))
        }
    }

    private fun getNoNeedPeriodItem(): BufferPeriodOption = BufferPeriodOption(
        period = Period(
            id = NO_NEED_PERIOD_ITEM_ID,
            interval = "",
            intervalCount = 0,
            enabled = false,
            displayName = context.getString(R.string.nc_do_not_need_buffer_period),
            isRecommended = false
        ),
        isSelected = false
    )

    fun onOptionClick(id: String) {
        val value = _state.value
        val options = value.options.toMutableList()
        val newOptions = options.map {
            it.copy(isSelected = it.period.id == id)
        }
        _state.update {
            it.copy(options = newOptions)
        }
    }

    fun onContinueClick() = viewModelScope.launch {
        val selectedPeriod = _state.value.options.first { it.isSelected }
        _event.emit(InheritanceBufferPeriodEvent.OnContinueClick(period = if (selectedPeriod.period.id == NO_NEED_PERIOD_ITEM_ID) null else selectedPeriod.period))
    }

    companion object {
        private const val NO_NEED_PERIOD_ITEM_ID = "-1"
    }
}