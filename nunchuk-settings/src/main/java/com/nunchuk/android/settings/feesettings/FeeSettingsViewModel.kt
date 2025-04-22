package com.nunchuk.android.settings.feesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.DEFAULT_FEE
import com.nunchuk.android.model.FreeRateOption
import com.nunchuk.android.usecase.GetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.GetDefaultFeeUseCase
import com.nunchuk.android.usecase.SetDefaultAntiFeeSnipingUseCase
import com.nunchuk.android.usecase.SetDefaultFeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeeSettingsViewModel @Inject constructor(
    private val getDefaultFeeUseCase: GetDefaultFeeUseCase,
    private val setDefaultFeeUseCase: SetDefaultFeeUseCase,
    private val getDefaultAntiFeeSnipingUseCase: GetDefaultAntiFeeSnipingUseCase,
    private val setDefaultAntiFeeSnipingUseCase: SetDefaultAntiFeeSnipingUseCase,
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
}

data class FeeSettingsState(
    val defaultFee: Int = FreeRateOption.ECONOMIC.ordinal,
    val antiFeeSniping: Boolean = false
)