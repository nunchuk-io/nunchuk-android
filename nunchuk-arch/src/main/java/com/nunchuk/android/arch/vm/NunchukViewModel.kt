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

package com.nunchuk.android.arch.vm

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

abstract class NunchukViewModel<State, Event> : ViewModel() {

    private val _state = MutableLiveData<State>()

    // TODO investigate side effect
    private val _event = MutableLiveData<Event>()

    protected abstract val initialState: State

    val state: LiveData<State> get() = _state

    val event: LiveData<Event> get() = _event

    @MainThread
    protected fun updateState(updater: State.() -> State) {
        _state.value = updater(_state.value ?: initialState)
    }

    @WorkerThread
    protected fun postState(updater: State.() -> State) {
        _state.postValue(updater(_state.value ?: initialState))
    }

    protected fun setEvent(event: Event) {
        _event.value = event
    }

    protected fun event(event: Event) {
        _event.postValue(event)
    }

    protected fun getState() = _state.value ?: initialState

    protected fun sendErrorEvent(roomId: String, t: Throwable, executable: (String, Throwable) -> Flow<Unit>) {
        viewModelScope.launch {
            executable(roomId, t)
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { }
        }
    }
}