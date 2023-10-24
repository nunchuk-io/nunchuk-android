package com.nunchuk.android.main.membership.custom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.settings.GetChainSettingFlowUseCase
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.Chain
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.signer.GetCurrentIndexFromMasterSignerUseCase
import com.nunchuk.android.usecase.signer.GetSignerFromMasterSignerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomKeyAccountFragmentViewModel @Inject constructor(
    private val getCurrentIndexFromMasterSignerUseCase: GetCurrentIndexFromMasterSignerUseCase,
    private val getSignerFromMasterSignerUseCase: GetSignerFromMasterSignerUseCase,
    private val getChainSettingFlowUseCase: GetChainSettingFlowUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val args: CustomKeyAccountFragmentFragmentArgs =
        CustomKeyAccountFragmentFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val _event = MutableSharedFlow<CustomKeyAccountFragmentEvent>()
    val event = _event.asSharedFlow()

    private val _state = MutableStateFlow(CustomKeyAccountUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getCurrentIndexFromMasterSignerUseCase(
                GetCurrentIndexFromMasterSignerUseCase.Param(
                    xfp = args.signer.fingerPrint,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT
                )
            ).onSuccess {
                _state.update { state -> state.copy(currentIndex = it) }
            }
        }
        viewModelScope.launch {
            getChainSettingFlowUseCase(Unit)
                .map { it.getOrDefault(Chain.MAIN) }
                .collect {
                    _state.update { state -> state.copy(isTestNet = it == Chain.TESTNET) }
                }
        }
    }

    fun checkSignerIndex(index: Int) {
        viewModelScope.launch {
            getSignerFromMasterSignerUseCase(
                GetSignerFromMasterSignerUseCase.Param(
                    xfp = args.signer.fingerPrint,
                    walletType = WalletType.MULTI_SIG,
                    addressType = AddressType.NATIVE_SEGWIT,
                    index = index
                )
            ).onSuccess { signer ->
                _event.emit(CustomKeyAccountFragmentEvent.CheckSigner(signer))
            }.onFailure {
                _event.emit(CustomKeyAccountFragmentEvent.CheckSigner(null))
            }
        }
    }
}

sealed class CustomKeyAccountFragmentEvent {
    data class CheckSigner(val signer: SingleSigner?) : CustomKeyAccountFragmentEvent()
}

data class CustomKeyAccountUiState(val currentIndex: Int = 0, val isTestNet: Boolean = false)

