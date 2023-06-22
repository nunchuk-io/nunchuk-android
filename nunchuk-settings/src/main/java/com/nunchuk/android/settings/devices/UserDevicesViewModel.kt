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

package com.nunchuk.android.settings.devices

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.profile.CompromiseUserDevicesUseCase
import com.nunchuk.android.core.profile.DeleteUserDevicesUseCase
import com.nunchuk.android.core.profile.GetUserDevicesUseCase
import com.nunchuk.android.core.profile.UserDeviceResponse
import com.nunchuk.android.utils.DeviceManager
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class UserDevicesViewModel @Inject constructor(
    private val deviceManager: DeviceManager,
    private val getUserDevicesUseCase: GetUserDevicesUseCase,
    private val deleteUserDevicesUseCase: DeleteUserDevicesUseCase,
    private val compromiseUserDevicesUseCase: CompromiseUserDevicesUseCase
) : NunchukViewModel<UserDeviceState, UserDeviceEvent>() {

    override val initialState = UserDeviceState()

    private fun getCurrentDeviceId(): String {
        return deviceManager.getDeviceId()
    }

    private fun findCurrentDevice(devices: List<UserDeviceResponse>): UserDeviceResponse? {
        return devices.find { it.id == getCurrentDeviceId() }
    }

    fun getCurrentDeviceList() = state.value?.devices.orEmpty()

    fun getUserDevices() {
        viewModelScope.launch {
            getUserDevicesUseCase.execute()
                .onStart {
                    event(UserDeviceEvent.Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException {
                    event(UserDeviceEvent.GetDevicesErrorEvent)
                }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    event(UserDeviceEvent.Loading(false))
                }
                .collect {
                    val newSortedDevices = generateNewSortedDevices(it)
                    updateState {
                        copy(
                            devices = newSortedDevices
                        )
                    }
                }
        }
    }

    private fun generateNewSortedDevices(devices: List<UserDeviceResponse>): List<UserDeviceResponse> {
        val currentDevice = findCurrentDevice(devices)
        val result = mutableListOf<UserDeviceResponse>()
        currentDevice?.let { device ->
            result.add(device)
        }

        val oldSortedDevices = devices.sortedByDescending { device -> device.lastTs }.toMutableList()
        oldSortedDevices.remove(currentDevice)

        result.addAll(oldSortedDevices)
        return result
    }

    fun deleteDevices(devices: List<UserDeviceResponse>) {
        if (devices.isEmpty()) {
            return
        }
        viewModelScope.launch {
            deleteUserDevicesUseCase.execute(devices.map { it.id.orEmpty() })
                .onStart { event(UserDeviceEvent.Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (devices.size == 1) {
                        event(UserDeviceEvent.DeleteDevicesSuccessEvent(devices.first()))
                    } else {
                        event(UserDeviceEvent.SignOutAllSuccessEvent)
                    }
                }
        }
    }

    fun markCompromised(devices: List<UserDeviceResponse>) {
        if (devices.isEmpty()) {
            return
        }
        viewModelScope.launch {
            compromiseUserDevicesUseCase.execute(devices.map { it.id.orEmpty() })
                .onStart { event(UserDeviceEvent.Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException { }
                .flowOn(Dispatchers.Main)
                .collect {
                    if (devices.size == 1) {
                        event(UserDeviceEvent.CompromisedDevicesSuccessEvent(devices.first()))
                    }
                }
        }
    }

}
