package com.nunchuk.android.settings.walletsecurity.decoy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.domain.GetWalletPinUseCase
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.nativelib.NunchukNativeSdk
import com.nunchuk.android.usecase.GetWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DecoyPinViewModel @Inject constructor(
    private val getWalletPinUseCase: GetWalletPinUseCase,
    private val nativeSdk: NunchukNativeSdk,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(DecoyPinUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            getWalletPinUseCase(Unit)
                .map { it.getOrDefault("") }
                .collect { pin ->
                    _state.update { it.copy(walletPin = pin) }
                }
        }

        viewModelScope.launch {
            getWalletsUseCase.execute()
                .zip(getAssistedWalletsFlowUseCase(Unit)) { wallets, assistedWallets ->
                    wallets to assistedWallets.getOrDefault(emptyList())
                }
                .collect { (wallets, assistedWallets) ->
                    val filterOutAssistedWallets = wallets.filter { wallet ->
                        assistedWallets.none { it.localId == wallet.wallet.id }
                    }
                    _state.update { it.copy(hasWallet = filterOutAssistedWallets.isNotEmpty()) }
                }
        }
    }

    fun getHashedPin(pin: String): String {
        return nativeSdk.hashSHA256(pin)
    }
}

data class DecoyPinUiState(
    val walletPin: String = "",
    val hasWallet: Boolean = false
)