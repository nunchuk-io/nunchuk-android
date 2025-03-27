package com.nunchuk.android.settings.feesettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.DEFAULT_FEE
import com.nunchuk.android.model.FreeRateOption
import com.nunchuk.android.usecase.GetDefaultFeeUseCase
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
    private val setDefaultFeeUseCase: SetDefaultFeeUseCase
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
    }

    fun setDefaultFee(fee: Int) = viewModelScope.launch {
        setDefaultFeeUseCase(fee).onSuccess {
            _state.update { it.copy(defaultFee = fee) }
            DEFAULT_FEE = fee
        }
    }
}

data class FeeSettingsState(
    val defaultFee: Int = FreeRateOption.ECONOMIC.ordinal
)