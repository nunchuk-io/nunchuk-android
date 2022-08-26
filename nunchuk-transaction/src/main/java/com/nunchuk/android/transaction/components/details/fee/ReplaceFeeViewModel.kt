package com.nunchuk.android.transaction.components.details.fee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.EstimateFeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ReplaceFeeViewModel @Inject constructor(
    private val estimateFeeUseCase: EstimateFeeUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ReplaceFeeState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val result = estimateFeeUseCase(Unit)
            if (result.isSuccess) {
                _state.value = ReplaceFeeState(estimateFeeRates = result.getOrThrow())
            }
        }
    }
}