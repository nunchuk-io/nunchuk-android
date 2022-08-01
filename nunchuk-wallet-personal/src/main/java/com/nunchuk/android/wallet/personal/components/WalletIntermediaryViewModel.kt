package com.nunchuk.android.wallet.personal.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WalletIntermediaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCompoundSignersUseCase: Lazy<GetCompoundSignersUseCase>
) : ViewModel() {
    private val _state = MutableStateFlow(WalletIntermediaryState())

    init {
        val args = WalletIntermediaryFragmentArgs.fromSavedStateHandle(savedStateHandle)
        if (args.isQuickWallet) {
            viewModelScope.launch {
                getCompoundSignersUseCase.get().execute().collect {
                    _state.value = WalletIntermediaryState(it.first.isNotEmpty() || it.second.isNotEmpty())
                }
            }
        }
    }

    val hasSigner: Boolean
        get() = _state.value.isHasSigner
}

data class WalletIntermediaryState(val isHasSigner: Boolean = false)

