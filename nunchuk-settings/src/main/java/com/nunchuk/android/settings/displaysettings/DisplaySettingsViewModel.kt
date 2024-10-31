package com.nunchuk.android.settings.displaysettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.model.ThemeMode
import com.nunchuk.android.usecase.GetUseLargeFontHomeBalancesUseCase
import com.nunchuk.android.usecase.SetUseLargeFontHomeBalancesUseCase
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
    private val getUseLargeFontHomeBalancesUseCase: GetUseLargeFontHomeBalancesUseCase,
    private val setUseLargeFontHomeBalancesUseCase: SetUseLargeFontHomeBalancesUseCase,
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase,
    private val getDarkModeUseCase: GetDarkModeUseCase,
    private val setDarkModeUseCase: SetDarkModeUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DisplaySettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getUseLargeFontHomeBalancesUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(largeFont = result.getOrDefault(false)) }
                }
        }
        viewModelScope.launch {
            getDarkModeUseCase(Unit)
                .collect { result ->
                    _state.update { it.copy(themeMode = result.getOrDefault(ThemeMode.System)) }
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
}

data class DisplaySettingsUiState(
    val largeFont: Boolean = false,
    val unit: Int = CURRENT_DISPLAY_UNIT_TYPE,
    val themeMode: ThemeMode = ThemeMode.System
)