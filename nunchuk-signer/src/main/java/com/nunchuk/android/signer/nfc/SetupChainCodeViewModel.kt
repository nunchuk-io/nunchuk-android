package com.nunchuk.android.signer.nfc

import android.nfc.tech.IsoDep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GenerateRandomChainCodeUseCase
import com.nunchuk.android.core.domain.SetupSatsCardUseCase
import com.nunchuk.android.core.util.CHAIN_CODE_LENGTH
import com.nunchuk.android.model.SatsCardStatus
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupChainCodeViewModel @Inject constructor(
    private val generateRandomChainCodeUseCase: GenerateRandomChainCodeUseCase,
    private val setupSatsCardUseCase: Lazy<SetupSatsCardUseCase>
) : ViewModel() {
    private val _state = MutableStateFlow<SetupChainCodeState?>(null)
    private val _event = Channel<SetupChainCodeEvent>()
    val state = _state.filterIsInstance<SetupChainCodeState>()
    val event = _event.receiveAsFlow()

    init {
        generateChainCode()
    }

    fun generateChainCode() {
        viewModelScope.launch {
            val result = generateRandomChainCodeUseCase(Unit)
            if (result.isSuccess) {
                _state.value = SetupChainCodeState(result.getOrThrow())
            } else {
                _event.send(SetupChainCodeEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun setUpSatsCard(isoDep: IsoDep?, cvc: String, chainCode: String) {
        isoDep ?: return
        viewModelScope.launch {
            _event.send(SetupChainCodeEvent.NfcLoading(true))
            val result = setupSatsCardUseCase.get()(SetupSatsCardUseCase.Data(isoDep, cvc, chainCode))
            _event.send(SetupChainCodeEvent.NfcLoading(false))
            if (result.isSuccess) {
                _event.send(SetupChainCodeEvent.SetupSatsCardSuccess(result.getOrThrow()))
            } else {
                _event.send(SetupChainCodeEvent.ShowError(result.exceptionOrNull()))
            }
        }
    }

    fun isValidChainCode(chainCode: String): Boolean {
        if (chainCode.length != CHAIN_CODE_LENGTH) return false
        return chainCode.all { c -> c in '0'..'9' || c in 'a'..'f' }
    }
}

sealed class SetupChainCodeEvent {
    class NfcLoading(val isLoading: Boolean) : SetupChainCodeEvent()
    class SetupSatsCardSuccess(val status: SatsCardStatus) : SetupChainCodeEvent()
    class ShowError(val e: Throwable?) : SetupChainCodeEvent()
}

data class SetupChainCodeState(
    val chainCode: String
)