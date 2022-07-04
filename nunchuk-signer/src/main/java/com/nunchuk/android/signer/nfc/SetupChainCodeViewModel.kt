package com.nunchuk.android.signer.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateRandomChainCodeUseCase
import com.nunchuk.android.core.util.CHAIN_CODE_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupChainCodeViewModel @Inject constructor(
    private val generateRandomChainCodeUseCase: GenerateRandomChainCodeUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<SetupChainCodeState?>(null)
    val state = _state.filterIsInstance<SetupChainCodeState>()

    init {
        generateChainCode()
    }

    fun generateChainCode() {
        viewModelScope.launch {
            val result = generateRandomChainCodeUseCase(Unit)
            if (result.isSuccess) {
                _state.value = SetupChainCodeState(result.getOrThrow())
            }
        }
    }

    fun isValidChainCode(chainCode: String) : Boolean {
        if (chainCode.length != CHAIN_CODE_LENGTH) return false
        return chainCode.all { c -> c in '0'..'9' || c in 'a'..'f' }
    }
}

data class SetupChainCodeState(
    val chainCode: String
)