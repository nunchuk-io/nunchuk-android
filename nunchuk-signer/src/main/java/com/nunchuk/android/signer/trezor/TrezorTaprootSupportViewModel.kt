package com.nunchuk.android.signer.trezor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.usecase.signer.GetSupportedSignersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrezorTaprootSupportState(
    val isLoaded: Boolean = false,
    val supportAllWalletTypes: Boolean = false,
    val supportedWalletTypes: Set<WalletType> = emptySet(),
) {
    fun isTaprootSupported(walletType: WalletType): Boolean {
        return isLoaded && (supportAllWalletTypes || walletType in supportedWalletTypes)
    }
}

@HiltViewModel
class TrezorTaprootSupportViewModel @Inject constructor(
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TrezorTaprootSupportState())
    val state = _state.asStateFlow()

    init {
        fetchTaprootSupport()
    }

    private fun fetchTaprootSupport() {
        viewModelScope.launch {
            getSupportedSignersUseCase(Unit)
                .onSuccess { supportedSigners ->
                    val trezorTaprootSigners = supportedSigners.filter { signer ->
                        signer.tag == SignerTag.TREZOR && signer.addressType == AddressType.TAPROOT
                    }

                    _state.update {
                        it.copy(
                            isLoaded = true,
                            supportAllWalletTypes = trezorTaprootSigners.any { signer -> signer.walletType == null },
                            supportedWalletTypes = trezorTaprootSigners.mapNotNull { signer -> signer.walletType }.toSet()
                        )
                    }
                }
                .onFailure {
                    _state.update { current -> current.copy(isLoaded = true) }
                }
        }
    }
}
