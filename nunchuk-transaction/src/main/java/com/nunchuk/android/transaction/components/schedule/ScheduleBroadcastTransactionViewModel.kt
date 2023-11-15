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

package com.nunchuk.android.transaction.components.schedule

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.membership.ScheduleBroadcastTransactionUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.manager.AssistedWalletManager
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.schedule.timezone.TimeZoneDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class ScheduleBroadcastTransactionViewModel @Inject constructor(
    private val application: Application,
    private val scheduleBroadcastTransactionUseCase: ScheduleBroadcastTransactionUseCase,
    private val assistedWalletManager: AssistedWalletManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args =
        ScheduleBroadcastTransactionFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<ScheduleBroadcastTransactionEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(ScheduleBroadcastTransactionState())
    val state = _state.asStateFlow()

    fun onSelectEvent(event: ScheduleBroadcastTransactionEvent) {
        viewModelScope.launch {
            _event.emit(event)
        }
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _state.value.time
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
        }
        _state.update {
            it.copy(time = cal.timeInMillis)
        }
    }

    fun setTime(hourOfDay: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _state.value.time
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
        }
        _state.update {
            it.copy(time = cal.timeInMillis)
        }
    }

    fun setTimeZone(zone: TimeZoneDetail) {
        _state.update { it.copy(timeZone = zone) }
    }

    fun onSaveClicked() {
        viewModelScope.launch {
            val selectedTimeZone = TimeZone.getTimeZone(_state.value.timeZone.id)
            val nextYear = Calendar.getInstance().apply {
                timeZone = selectedTimeZone
                add(Calendar.YEAR, 1)
            }
            val selectedCalInCurrentZone = Calendar.getInstance().apply {
                timeInMillis = _state.value.time
            }
            val selectedTime = Calendar.getInstance().apply {
                timeZone = selectedTimeZone
                set(Calendar.YEAR, selectedCalInCurrentZone.get(Calendar.YEAR))
                set(Calendar.MONTH, selectedCalInCurrentZone.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, selectedCalInCurrentZone.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR, selectedCalInCurrentZone.get(Calendar.HOUR))
                set(Calendar.MINUTE, selectedCalInCurrentZone.get(Calendar.MINUTE))
            }
            if (selectedTime.timeInMillis > nextYear.timeInMillis) {
                _event.emit(ScheduleBroadcastTransactionEvent.ShowError(application.getString(R.string.nc_error_schedule_broadcast_out_of_range)))
            } else {
                _event.emit(ScheduleBroadcastTransactionEvent.Loading(true))
                val result = scheduleBroadcastTransactionUseCase(
                    ScheduleBroadcastTransactionUseCase.Param(
                        groupId = assistedWalletManager.getGroupId(args.walletId),
                        walletId = args.walletId,
                        transactionId = args.transactionId,
                        scheduleTime = selectedTime.timeInMillis,
                    )
                )
                _event.emit(ScheduleBroadcastTransactionEvent.Loading(false))
                if (result.isSuccess) {
                    val serverTransaction = result.getOrThrow()
                    if (serverTransaction.spendingLimitMessage.isEmpty()) {
                        _event.emit(ScheduleBroadcastTransactionEvent.ScheduleBroadcastSuccess(serverTransaction))
                    } else {
                        _event.emit(ScheduleBroadcastTransactionEvent.ShowError(serverTransaction.spendingLimitMessage))
                    }
                } else {
                    _event.emit(ScheduleBroadcastTransactionEvent.ShowError(result.exceptionOrNull()?.message.orUnknownError()))
                }
            }
        }
    }
}

sealed class ScheduleBroadcastTransactionEvent {
    object OnSelectDateEvent : ScheduleBroadcastTransactionEvent()
    object OnSelectTimeEvent : ScheduleBroadcastTransactionEvent()
    object OnSelectTimeZoneEvent : ScheduleBroadcastTransactionEvent()
    data class ShowError(val message: String) : ScheduleBroadcastTransactionEvent()
    data class Loading(val isLoading: Boolean) : ScheduleBroadcastTransactionEvent()
    data class ScheduleBroadcastSuccess(val serverTransaction: ServerTransaction) : ScheduleBroadcastTransactionEvent()
}