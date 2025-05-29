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

package com.nunchuk.android.settings.network

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetRemoteElectrumServersCacheUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NetworkSettingViewModel @Inject constructor(
    private val initAppSettingsUseCase: InitAppSettingsUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val sendSignOutUseCase: SendSignOutUseCase,
    private val appScope: CoroutineScope,
    private val clearInfoSessionUseCase: ClearInfoSessionUseCase,
    private val getRemoteElectrumServersCacheUseCase: GetRemoteElectrumServersCacheUseCase,
    private val signInModeHolder: SignInModeHolder,
) : NunchukViewModel<NetworkSettingState, NetworkSettingEvent>() {

    override val initialState = NetworkSettingState()

    val currentAppSettings: AppSettings?
        get() = state.value?.appSetting

    val customMainnetServerName: String?
        get() = state.value?.customMainnetServerName

    var initAppSettings: AppSettings? = null

    init {
        viewModelScope.launch {
            getAppSettingUseCase(Unit).onSuccess { appSettings ->
                initAppSettings = appSettings
                updateCurrentState(appSettings)
                loadCustomMainnetServer(appSettings.mainnetServers.firstOrNull())
                setEvent(
                    NetworkSettingEvent.ResetTextHostServerEvent(appSettings)
                )
            }
        }
    }

    private suspend fun loadCustomMainnetServer(url: String?) {
        val servers =
            getRemoteElectrumServersCacheUseCase(Unit).map { it.getOrThrow() }.firstOrNull()
                .orEmpty()
        val customMainnetServer = servers.find { server -> server.url == url }
        updateState {
            copy(customMainnetServerName = customMainnetServer?.name)
        }
    }

    fun updateCurrentState(appSettings: AppSettings) {
        updateState {
            copy(appSetting = appSettings)
        }
    }

    fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            val result = updateAppSettingUseCase(appSettings)
            if (result.isSuccess) {
                initAppSettings = result.getOrThrow()
                updateState {
                    copy(appSetting = result.getOrThrow())
                }
                setEvent(NetworkSettingEvent.UpdateSettingSuccessEvent(result.getOrThrow()))
            }
        }
    }

    fun resetToDefaultAppSetting() {
        viewModelScope.launch {
            val result = initAppSettingsUseCase(Unit)
            result.getOrNull()?.let {
                initAppSettings = it
                updateState {
                    copy(appSetting = it)
                }
                setEvent(NetworkSettingEvent.ResetTextHostServerEvent(it))
                setEvent(NetworkSettingEvent.UpdateSettingSuccessEvent(it))
            }
        }
    }

    fun signOut() {
        appScope.launch {
            event(NetworkSettingEvent.LoadingEvent(true))
            signInModeHolder.clear()
            clearInfoSessionUseCase(Unit)
            sendSignOutUseCase(Unit)
            event(NetworkSettingEvent.SignOutSuccessEvent)
        }
    }
}