package com.nunchuk.android.settings.walletvisibilitysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.model.setting.HomeDisplaySetting
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.usecase.GetHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.UpdateHomeDisplaySettingUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletVisibilitySettingsViewModel @Inject constructor(
    private val getHomeDisplaySettingUseCase: GetHomeDisplaySettingUseCase,
    private val updateHomeDisplaySettingUseCase: UpdateHomeDisplaySettingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WalletVisibilitySettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getHomeDisplaySettingUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(homeDisplaySetting = result.getOrNull() ?: HomeDisplaySetting()) }
                }
        }
    }

    fun onFontSizeChange(largeFont: Boolean) = viewModelScope.launch {
        val homeDisplaySetting = _state.value.homeDisplaySetting.copy(useLargeFont = largeFont)
        updateHomeDisplaySetting(homeDisplaySetting)
    }

    fun onDisplayTotalBalanceChange(displayTotalBalance: Boolean) = viewModelScope.launch {
        val homeDisplaySetting = _state.value.homeDisplaySetting.copy(showTotalBalance = displayTotalBalance)
        updateHomeDisplaySetting(homeDisplaySetting)
    }

    fun onDisplayWalletShortcutsChange(displayWalletShortcuts: Boolean) = viewModelScope.launch {
        val homeDisplaySetting = _state.value.homeDisplaySetting.copy(showWalletShortcuts = displayWalletShortcuts)
        updateHomeDisplaySetting(homeDisplaySetting)
    }

    private fun updateHomeDisplaySetting(homeDisplaySetting: HomeDisplaySetting) {
        viewModelScope.launch {
            updateHomeDisplaySettingUseCase(homeDisplaySetting)
                .onSuccess {
                    _state.update { it.copy(homeDisplaySetting = homeDisplaySetting) }
                }
        }
    }
}

data class WalletVisibilitySettingsUiState(
    val homeDisplaySetting: HomeDisplaySetting = HomeDisplaySetting(),
)