package com.nunchuk.android.settings.network

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.core.domain.InitAppSettingsUseCase
import com.nunchuk.android.core.domain.UpdateAppSettingUseCase
import com.nunchuk.android.model.AppSettings
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class NetworkSettingViewModel @Inject constructor(
    private val initAppSettingsUseCase: InitAppSettingsUseCase,
    private val updateAppSettingUseCase: UpdateAppSettingUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase
) : NunchukViewModel<NetworkSettingState, NetworkSettingEvent>() {

    override val initialState = NetworkSettingState()

    val currentAppSettings: AppSettings?
        get() = state.value?.appSetting

    var initAppSettings: AppSettings? = null

    fun updateCurrentState(appSettings: AppSettings) {
        updateState {
            copy(appSetting = appSettings)
        }
    }

    fun fireResetTextHostServerEvent(appSettings: AppSettings) {
        event(
            NetworkSettingEvent.ResetTextHostServerEvent(appSettings)
        )
    }

    fun getAppSettings() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    initAppSettings = it
                    updateState {
                        copy(appSetting = it)
                    }
                    event(
                        NetworkSettingEvent.ResetTextHostServerEvent(it)
                    )
                }
        }
    }

    fun updateAppSettings(appSettings: AppSettings) {
        viewModelScope.launch {
            updateAppSettingUseCase.execute(appSettings)
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    initAppSettings = it
                    updateState {
                        copy(appSetting = it)
                    }
                    event(NetworkSettingEvent.UpdateSettingSuccessEvent(it))
                }
        }
    }

    fun resetToDefaultAppSetting() {
        viewModelScope.launch {
            initAppSettingsUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    initAppSettings = it
                    updateState {
                        copy(appSetting = it)
                    }
                    event(NetworkSettingEvent.UpdateSettingSuccessEvent(it))
                }
        }
    }
}