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
import com.nunchuk.android.core.constants.Constants.MAIN_NET_HOST
import com.nunchuk.android.core.constants.Constants.SIG_NET_HOST
import com.nunchuk.android.core.constants.Constants.TEST_NET_HOST
import com.nunchuk.android.core.constants.defaultLiquidServers
import com.nunchuk.android.core.domain.ClearInfoSessionUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.GetRemoteElectrumServersCacheUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.core.guestmode.SignInModeHolder
import com.nunchuk.android.core.persistence.NCSharePreferences
import com.nunchuk.android.core.profile.SendSignOutUseCase
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.type.Chain
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
    private val ncSharePreferences: NCSharePreferences,
) : NunchukViewModel<NetworkSettingState, NetworkSettingEvent>() {

    override val initialState = NetworkSettingState()

    val currentAppSettings: AppSettings?
        get() = state.value?.appSetting

    val customMainnetServerName: String?
        get() = state.value?.customMainnetServerName

    var initAppSettings: AppSettings? = null
    private var initMainnetServer: String = ""
    private var initTestnetServer: String = ""
    private var initSignetServer: String = ""

    init {
        viewModelScope.launch {
            getAppSettingUseCase(Unit).onSuccess { appSettings ->
                val mainnet = ncSharePreferences.customMainnetServer.ifBlank { MAIN_NET_HOST }
                val testnet = ncSharePreferences.customTestnetServer.ifBlank { TEST_NET_HOST }
                val signet = ncSharePreferences.customSignetServer.ifBlank { SIG_NET_HOST }
                initAppSettings = appSettings
                initMainnetServer = mainnet
                initTestnetServer = testnet
                initSignetServer = signet
                updateState {
                    copy(
                        appSetting = appSettings,
                        mainnetServer = mainnet,
                        testnetServer = testnet,
                        signetServer = signet,
                    )
                }
                loadCustomMainnetServer(mainnet)
                setEvent(
                    NetworkSettingEvent.ResetTextHostServerEvent(mainnet, testnet, signet)
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

    fun onHostChanged(chain: Chain, host: String) {
        val current = state.value ?: return
        val withHost = when (chain) {
            Chain.MAIN -> current.copy(mainnetServer = host)
            Chain.TESTNET -> current.copy(testnetServer = host)
            Chain.SIGNET -> current.copy(signetServer = host)
            else -> current
        }
        val withAppSetting = if (current.appSetting.chain == chain) {
            withHost.copy(appSetting = withHost.appSetting.copy(electrumServers = listOf(host)))
        } else {
            withHost
        }
        updateState { withAppSetting }
    }

    fun onChainChanged(chain: Chain) {
        val current = state.value ?: return
        val newHost = when (chain) {
            Chain.MAIN -> current.mainnetServer
            Chain.TESTNET -> current.testnetServer
            Chain.SIGNET -> current.signetServer
            else -> current.appSetting.electrumServers.firstOrNull().orEmpty()
        }
        updateState {
            copy(
                appSetting = appSetting.copy(
                    chain = chain,
                    electrumServers = listOf(newHost),
                    liquidServers = defaultLiquidServers(chain),
                )
            )
        }
    }

    fun onEnableProxyChanged(enable: Boolean) {
        val current = state.value ?: return
        if (current.appSetting.enableProxy == enable) return
        updateState {
            copy(
                appSetting = appSetting.copy(
                    enableProxy = enable,
                    // Seed the Orbot defaults so the toggle alone is a usable setup.
                    proxyHost = if (enable) appSetting.proxyHost.ifBlank { DEFAULT_PROXY_HOST } else appSetting.proxyHost,
                    proxyPort = if (enable && appSetting.proxyPort <= 0) {
                        DEFAULT_PROXY_PORT.toInt()
                    } else {
                        appSetting.proxyPort
                    },
                )
            )
        }
    }

    /**
     * Pull the proxy fields back in after the proxy screen may have written them.
     * Only those fields, so any unsaved chain/server edit on this screen survives.
     */
    fun refreshProxySetting() {
        viewModelScope.launch {
            val persisted = getAppSettingUseCase(Unit).getOrNull() ?: return@launch
            val current = state.value ?: return@launch
            val initial = initAppSettings ?: return@launch
            // onResume also fires on plain foregrounding, so keep an unsaved toggle
            // made here and only adopt what the proxy screen wrote.
            if (!current.appSetting.hasSameProxyAs(initial)) return@launch
            initAppSettings = initial.withProxyOf(persisted)
            updateState { copy(appSetting = appSetting.withProxyOf(persisted)) }
        }
    }

    private fun AppSettings.hasSameProxyAs(other: AppSettings) =
        enableProxy == other.enableProxy
                && proxyHost == other.proxyHost
                && proxyPort == other.proxyPort
                && proxyUsername == other.proxyUsername
                && proxyPassword == other.proxyPassword

    private fun AppSettings.withProxyOf(source: AppSettings) = copy(
        enableProxy = source.enableProxy,
        proxyHost = source.proxyHost,
        proxyPort = source.proxyPort,
        proxyUsername = source.proxyUsername,
        proxyPassword = source.proxyPassword,
    )

    fun hasPendingChanges(): Boolean {
        val current = state.value ?: return false
        return current.appSetting != initAppSettings
                || current.mainnetServer != initMainnetServer
                || current.testnetServer != initTestnetServer
                || current.signetServer != initSignetServer
    }

    fun saveCurrentSettings() {
        val current = state.value ?: return
        ncSharePreferences.customMainnetServer = current.mainnetServer
        ncSharePreferences.customTestnetServer = current.testnetServer
        ncSharePreferences.customSignetServer = current.signetServer
        viewModelScope.launch {
            // Re-read the persisted settings so fields owned by other screens (the
            // proxy settings) aren't clobbered by this screen's older snapshot.
            val persisted = getAppSettingUseCase(Unit).getOrNull() ?: current.appSetting
            updateAppSettings(
                persisted.copy(
                    chain = current.appSetting.chain,
                    electrumServers = current.appSetting.electrumServers,
                    liquidServers = current.appSetting.liquidServers,
                    // This screen owns the enable toggle (and the host/port it seeds);
                    // the credentials stay whatever the proxy screen last saved.
                    enableProxy = current.appSetting.enableProxy,
                    proxyHost = current.appSetting.proxyHost,
                    proxyPort = current.appSetting.proxyPort,
                )
            )
        }
    }

    private fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            val result = updateAppSettingUseCase(appSettings)
            if (result.isSuccess) {
                val saved = result.getOrThrow()
                initAppSettings = saved
                initMainnetServer = state.value?.mainnetServer.orEmpty()
                initTestnetServer = state.value?.testnetServer.orEmpty()
                initSignetServer = state.value?.signetServer.orEmpty()
                updateState {
                    copy(appSetting = saved)
                }
                setEvent(NetworkSettingEvent.UpdateSettingSuccessEvent(saved))
            }
        }
    }

    fun resetToDefaultAppSetting() {
        viewModelScope.launch {
            ncSharePreferences.customMainnetServer = MAIN_NET_HOST
            ncSharePreferences.customTestnetServer = TEST_NET_HOST
            ncSharePreferences.customSignetServer = SIG_NET_HOST
            val result = initAppSettingsUseCase(Unit)
            result.getOrNull()?.let {
                initAppSettings = it
                initMainnetServer = MAIN_NET_HOST
                initTestnetServer = TEST_NET_HOST
                initSignetServer = SIG_NET_HOST
                updateState {
                    copy(
                        appSetting = it,
                        mainnetServer = MAIN_NET_HOST,
                        testnetServer = TEST_NET_HOST,
                        signetServer = SIG_NET_HOST,
                    )
                }
                setEvent(
                    NetworkSettingEvent.ResetTextHostServerEvent(
                        MAIN_NET_HOST,
                        TEST_NET_HOST,
                        SIG_NET_HOST,
                    )
                )
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
