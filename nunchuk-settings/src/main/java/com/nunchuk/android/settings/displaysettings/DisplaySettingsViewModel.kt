package com.nunchuk.android.settings.displaysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.model.ThemeMode
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.darkmode.GetDarkModeUseCase
import com.nunchuk.android.usecase.darkmode.SetDarkModeUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase,
    private val getDarkModeUseCase: GetDarkModeUseCase,
    private val setDarkModeUseCase: SetDarkModeUseCase,
    private val updateWalletSecuritySettingUseCase: UpdateWalletSecuritySettingUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DisplaySettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getDarkModeUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(themeMode = result.getOrDefault(ThemeMode.System)) }
                }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(walletSecuritySetting = result.getOrNull() ?: WalletSecuritySetting()) }
                }
        }
    }

    fun getDisplayUnitSetting() {
        viewModelScope.launch {
            getDisplayUnitSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { unit ->
                    _state.update { it.copy(unit = unit.getCurrentDisplayUnitType()) }
                }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            setDarkModeUseCase(mode.value)
                .onSuccess {
                    _state.update { it.copy(themeMode = mode) }
                }
        }
    }

    fun updateHideWalletDetail() = viewModelScope.launch {
        val walletSecuritySetting = _state.value.walletSecuritySetting
        val hideWalletDetail = walletSecuritySetting.hideWalletDetail.not()
        updateSetting(walletSecuritySetting.copy(hideWalletDetail = hideWalletDetail))
    }

    private suspend fun updateSetting(walletSecuritySetting: WalletSecuritySetting) {
        val result = updateWalletSecuritySettingUseCase(walletSecuritySetting)
        if (result.isSuccess) {
            _state.update { it.copy(walletSecuritySetting = walletSecuritySetting) }
        }
    }
}

data class DisplaySettingsUiState(
    val unit: Int = CURRENT_DISPLAY_UNIT_TYPE,
    val themeMode: ThemeMode = ThemeMode.System,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
)