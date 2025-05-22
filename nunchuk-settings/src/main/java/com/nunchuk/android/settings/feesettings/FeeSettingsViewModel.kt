package com.nunchuk.android.settings.feesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.DEFAULT_FEE
import com.nunchuk.android.model.FreeRateOption
import com.nunchuk.android.model.setting.TaprootFeeSelectionSetting
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.GetDefaultFeeUseCase
import com.nunchuk.android.usecase.GetTaprootSelectionFeeSettingUseCase
import com.nunchuk.android.usecase.SetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.SetDefaultFeeUseCase
import com.nunchuk.android.usecase.SetTaprootSelectionFeeSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FeeSettingsViewModel @Inject constructor(
    private val getDefaultFeeUseCase: GetDefaultFeeUseCase,
    private val setDefaultFeeUseCase: SetDefaultFeeUseCase,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
    private val setDefaultAntiFeeSnipingUseCase: SetDefaultAntiFeeSnipingUseCase,
    private val getTaprootSelectionFeeSettingUseCase: GetTaprootSelectionFeeSettingUseCase,
    private val setTaprootSelectionFeeSettingUseCase: SetTaprootSelectionFeeSettingUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(FeeSettingsState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getDefaultFeeUseCase(Unit)
                .collect { result ->
                    _state.update {
                        it.copy(defaultFee = result.getOrDefault(FreeRateOption.ECONOMIC.ordinal))
                    }
                }
        }
        viewModelScope.launch {
            getDefaultAntiFeeSnipingUseCase(Unit)
                .collect { result ->
                    _state.update {
                        it.copy(antiFeeSniping = result.getOrDefault(false))
                    }
                }
        }
        viewModelScope.launch {
            getTaprootSelectionFeeSettingUseCase(Unit)
                .collect { result ->
                    val taprootFeeSetting = result.getOrThrow()
                    _state.update {
                        it.copy(
                            automaticFee = taprootFeeSetting.automaticFeeEnabled,
                            taprootPercentage = taprootFeeSetting.feeDifferenceThresholdPercent.toString(),
                            taprootAmount = String.format(Locale.US, "%.2f", taprootFeeSetting.feeDifferenceThresholdCurrency),
                            isFirstTimeSettingTaprootFee = taprootFeeSetting.isFirstTime
                        )
                    }
                }
        }
    }

    fun setDefaultFee(fee: Int) = viewModelScope.launch {
        setDefaultFeeUseCase(fee).onSuccess {
            _state.update { it.copy(defaultFee = fee) }
            DEFAULT_FEE = fee
        }
    }

    fun setAntiFeeSniping(enable: Boolean) = viewModelScope.launch {
        setDefaultAntiFeeSnipingUseCase(enable).onSuccess {
            _state.update { it.copy(antiFeeSniping = enable) }
        }
    }

    fun saveTaprootSelectionFeeSetting(
        enabled: Boolean,
        taprootPercentage: String,
        taprootAmount: String,
    ) = viewModelScope.launch {
        val percentage = taprootPercentage.toIntOrNull() ?: 0
        val amount = taprootAmount
        setTaprootSelectionFeeSettingUseCase(
            TaprootFeeSelectionSetting(
                automaticFeeEnabled = enabled,
                feeDifferenceThresholdPercent = percentage,
                feeDifferenceThresholdCurrency = amount.toDoubleOrNull() ?: 0.0,
                isFirstTime = false
            )
        ).onSuccess {
            _state.update {
                it.copy(
                    isFirstTimeSettingTaprootFee = false,
                    automaticFee = enabled,
                    taprootPercentage = percentage.toString(),
                    taprootAmount = amount
                )
            }
        }
    }
}

data class FeeSettingsState(
    val defaultFee: Int = FreeRateOption.ECONOMIC.ordinal,
    val antiFeeSniping: Boolean = false,
    val automaticFee: Boolean = false,
    val taprootPercentage: String = "",
    val taprootAmount: String = "",
    val isFirstTimeSettingTaprootFee: Boolean = false,
)