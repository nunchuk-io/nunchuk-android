package com.nunchuk.android.wallet.components.coin.tag

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CoinTagSelectColorBottomSheetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val args =
        CoinTagSelectColorBottomSheetFragmentArgs.fromSavedStateHandle(savedStateHandle)

    private val _state = MutableStateFlow(CoinTagSelectColorBottomSheetState())
    val state = _state.asStateFlow()

    init {
        _state.update { it.copy(selectedColor = args.selectedColor) }
    }

    fun getSelectedColor() = _state.value.selectedColor

    fun updateSelectedColor(value: String) {
        _state.update { it.copy(selectedColor = value) }
    }

}

data class CoinTagSelectColorBottomSheetState(val selectedColor: String = "")