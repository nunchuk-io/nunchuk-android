package com.nunchuk.android.settings.walletvisibilitysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.setting.WalletSecuritySetting
import com.nunchuk.android.settings.walletsecurity.WalletSecuritySettingEvent
import com.nunchuk.android.usecase.GetDisplayTotalBalanceUseCase
import com.nunchuk.android.usecase.GetUseLargeFontHomeBalancesUseCase
import com.nunchuk.android.usecase.GetWalletSecuritySettingUseCase
import com.nunchuk.android.usecase.SetDisplayTotalBalanceUseCase
import com.nunchuk.android.usecase.SetUseLargeFontHomeBalancesUseCase
import com.nunchuk.android.usecase.UpdateWalletSecuritySettingUseCase
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
class WalletVisibilitySettingsViewModel @Inject constructor(
    private val getUseLargeFontHomeBalancesUseCase: GetUseLargeFontHomeBalancesUseCase,
    private val setUseLargeFontHomeBalancesUseCase: SetUseLargeFontHomeBalancesUseCase,
    private val getDisplayTotalBalanceUseCase: GetDisplayTotalBalanceUseCase,
    private val setDisplayTotalBalanceUseCase: SetDisplayTotalBalanceUseCase,
    private val updateWalletSecuritySettingUseCase: UpdateWalletSecuritySettingUseCase,
    private val getWalletSecuritySettingUseCase: GetWalletSecuritySettingUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(WalletVisibilitySettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getUseLargeFontHomeBalancesUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(largeFont = result.getOrDefault(false)) }
                }
        }
        viewModelScope.launch {
            getDisplayTotalBalanceUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(displayTotalBalance = result.getOrDefault(false)) }
                }
        }
        viewModelScope.launch {
            getWalletSecuritySettingUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(walletSecuritySetting = result.getOrNull() ?: WalletSecuritySetting()) }
                }
        }
    }

    fun onFontSizeChange(largeFont: Boolean) = viewModelScope.launch {
        setUseLargeFontHomeBalancesUseCase(largeFont)
            .onSuccess {
                _state.update { it.copy(largeFont = largeFont) }
            }
            .onFailure {
                _state.update { it.copy(largeFont = largeFont.not()) }
            }
    }

    fun onDisplayTotalBalanceChange(displayTotalBalance: Boolean) = viewModelScope.launch {
        setDisplayTotalBalanceUseCase(displayTotalBalance)
            .onSuccess {
                _state.update { it.copy(displayTotalBalance = displayTotalBalance) }
            }
            .onFailure {
                _state.update { it.copy(displayTotalBalance = displayTotalBalance.not()) }
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

data class WalletVisibilitySettingsUiState(
    val largeFont: Boolean = false,
    val displayTotalBalance: Boolean = false,
    val walletSecuritySetting: WalletSecuritySetting = WalletSecuritySetting(),
    )