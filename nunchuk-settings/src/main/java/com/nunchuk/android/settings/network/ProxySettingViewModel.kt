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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.model.StateEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SOCKS5 proxy for Electrum connections (e.g. Orbot on 127.0.0.1:9050).
 *
 * libnunchuk reads the proxy when it builds the Electrum client, so a change only
 * lands once the SDK is re-initialised - which happens in Application.onCreate.
 * Saving therefore restarts the app, but unlike a chain switch it does not need to
 * sign the user out.
 */
@HiltViewModel
class ProxySettingViewModel @Inject constructor(
    private val getAppSettingUseCase: GetAppSettingUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProxySettingUiState())
    val uiState = _uiState.asStateFlow()

    private var appSettings: AppSettings? = null

    init {
        viewModelScope.launch {
            getAppSettingUseCase(Unit).onSuccess { settings ->
                appSettings = settings
                _uiState.update {
                    it.copy(
                        enableProxy = settings.enableProxy,
                        host = settings.proxyHost,
                        port = settings.proxyPort.takeIf { port -> port > 0 }?.toString().orEmpty(),
                        username = settings.proxyUsername,
                        password = settings.proxyPassword,
                    )
                }
            }
        }
    }

    // Seed the Orbot defaults as real values, not just a placeholder: a greyed-out
    // hint reads as "already filled in", so Save would reject what the user sees.
    fun onEnableProxyChanged(enable: Boolean) = _uiState.update {
        it.copy(
            enableProxy = enable,
            host = if (enable) it.host.ifBlank { DEFAULT_PROXY_HOST } else it.host,
            port = if (enable) it.port.ifBlank { DEFAULT_PROXY_PORT } else it.port,
            hostError = null,
            portError = null,
        )
    }

    fun onHostChanged(host: String) = _uiState.update { it.copy(host = host, hostError = null) }

    fun onPortChanged(port: String) = _uiState.update {
        it.copy(port = port.filter(Char::isDigit).take(MAX_PORT_LENGTH), portError = null)
    }

    fun onUsernameChanged(username: String) = _uiState.update { it.copy(username = username) }

    fun onPasswordChanged(password: String) = _uiState.update { it.copy(password = password) }

    fun hasChanges(): Boolean {
        val settings = appSettings ?: return false
        val state = _uiState.value
        return settings.enableProxy != state.enableProxy
                || settings.proxyHost != state.host.trim()
                || settings.proxyPort != state.port.toIntOrNull().orZero()
                || settings.proxyUsername != state.username
                || settings.proxyPassword != state.password
    }

    fun save() {
        val settings = appSettings ?: return
        val state = _uiState.value
        val host = state.host.trim()
        val port = state.port.toIntOrNull().orZero()
        if (state.enableProxy) {
            if (host.isBlank()) {
                _uiState.update { it.copy(hostError = ProxyInputError.HOST_REQUIRED) }
                return
            }
            if (port !in MIN_PORT..MAX_PORT) {
                _uiState.update { it.copy(portError = ProxyInputError.PORT_INVALID) }
                return
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateAppSettingUseCase(
                settings.copy(
                    enableProxy = state.enableProxy,
                    proxyHost = host,
                    proxyPort = port,
                    proxyUsername = state.username,
                    proxyPassword = state.password,
                )
            ).onSuccess { saved ->
                appSettings = saved
                _uiState.update { it.copy(saveSuccessEvent = StateEvent.Unit) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onHandleSaveSuccessEvent() = _uiState.update { it.copy(saveSuccessEvent = StateEvent.None) }

    private fun Int?.orZero() = this ?: 0

    companion object {
        private const val MIN_PORT = 1
        private const val MAX_PORT = 65535
        private const val MAX_PORT_LENGTH = 5
    }
}

// Orbot's local SOCKS5 endpoint, the case this screen exists for.
internal const val DEFAULT_PROXY_HOST = "127.0.0.1"
internal const val DEFAULT_PROXY_PORT = "9050"

enum class ProxyInputError { HOST_REQUIRED, PORT_INVALID }

data class ProxySettingUiState(
    val enableProxy: Boolean = false,
    val host: String = "",
    val port: String = "",
    val username: String = "",
    val password: String = "",
    val hostError: ProxyInputError? = null,
    val portError: ProxyInputError? = null,
    val isLoading: Boolean = false,
    val saveSuccessEvent: StateEvent = StateEvent.None,
)
