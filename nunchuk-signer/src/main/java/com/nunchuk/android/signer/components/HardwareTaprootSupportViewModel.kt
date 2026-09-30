package com.nunchuk.android.signer.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.signer.SupportedSigner
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

/**
 * Which hardware keys the server reports as taproot-capable, for the shared
 * "Select wallet & address type" screen. Every device that reuses that screen (Trezor, Ledger,
 * BitBox, ...) reads its own row out of the same list, so the lookup is keyed by [SignerTag].
 */
data class HardwareTaprootSupportState(
    val isLoaded: Boolean = false,
    val taprootSigners: List<SupportedSigner> = emptyList(),
) {
    /**
     * The endpoint behind [taprootSigners] only ever returns taproot-capable keys, so a row that
     * names no address type is taproot-capable, and a row with no wallet type covers every wallet
     * type. Until the list has loaded (or if it failed to) taproot stays off — the key may not be
     * able to sign it, and offering it would create a wallet the device can't use.
     */
    fun isTaprootSupported(tag: SignerTag?, walletType: WalletType): Boolean {
        if (!isLoaded || tag == null) return false
        return taprootSigners.any { signer ->
            signer.tag == tag
                    && (signer.addressType == null || signer.addressType == AddressType.TAPROOT)
                    && (signer.walletType == null || signer.walletType == walletType)
        }
    }
}

@HiltViewModel
class HardwareTaprootSupportViewModel @Inject constructor(
    private val getSupportedSignersUseCase: GetSupportedSignersUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HardwareTaprootSupportState())
    val state = _state.asStateFlow()

    init {
        fetchTaprootSupport()
    }

    private fun fetchTaprootSupport() {
        viewModelScope.launch {
            getSupportedSignersUseCase(Unit)
                .onSuccess { supportedSigners ->
                    _state.update {
                        it.copy(isLoaded = true, taprootSigners = supportedSigners)
                    }
                }
                .onFailure {
                    _state.update { current -> current.copy(isLoaded = true) }
                }
        }
    }
}
