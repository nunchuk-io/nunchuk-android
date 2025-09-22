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

package com.nunchuk.android.transaction.components.schedule.timezone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.ui.TimeZoneDetail
import com.nunchuk.android.core.ui.toTimeZoneDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class SelectTimeZoneViewModel @Inject constructor(

) : ViewModel() {
    private val _event = MutableSharedFlow<SelectTimeZoneEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(SelectTimeZoneState())
    val state = _state.asStateFlow()

    private val timezones = mutableListOf<TimeZoneDetail>()
    private val search = MutableSharedFlow<String>()

    init {
        val timezones = TimeZone.getAvailableIDs().mapNotNull { id ->
            id.toTimeZoneDetail()
        }.sortedBy { it.offset }
        this.timezones.apply {
            clear()
            addAll(timezones)
        }
        _state.update {
            it.copy(timezones = timezones)
        }
        viewModelScope.launch {
            search.debounce(300L).collect { value ->
                if (value.isEmpty()) {
                    _state.update {
                        it.copy(timezones = timezones)
                    }
                } else {
                    _state.update {
                        it.copy(timezones = timezones.filter { timeZone ->
                            timeZone.city.contains(value)
                                    || timeZone.country.contains(value)
                                    || timeZone.offset.contains(value)
                        })
                    }
                }
            }
        }
    }

    fun onSearch(value: String) {
        viewModelScope.launch {
            search.emit(value)
        }
    }

    fun onTimeZoneClicked(zone: TimeZoneDetail) {
        viewModelScope.launch {
            _event.emit(SelectTimeZoneEvent.OnSelectTimeZone(zone))
        }
    }
}

sealed class SelectTimeZoneEvent {
    data class OnSelectTimeZone(val zone: TimeZoneDetail,) : SelectTimeZoneEvent()
}