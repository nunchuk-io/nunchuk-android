package com.nunchuk.android.app.referral.address

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.util.orUnknownError
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.wallet.WalletStatus
import com.nunchuk.android.usecase.CheckAddressValidUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.usecase.ParseBtcUriUseCase
import com.nunchuk.android.usecase.wallet.GetUnusedWalletAddressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralAddressViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getUnusedWalletAddressUseCase: GetUnusedWalletAddressUseCase,
    private val parseBtcUriUseCase: ParseBtcUriUseCase,
    private val checkAddressValidUseCase: CheckAddressValidUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ReferralAddressUiState())
    val state = _state.asStateFlow()

    private val preAddress: String? = savedStateHandle["address"]
        get() {
            return if (field == DEFAULT_ADDRESS) null else field
        }

    private val preWalletId: String? = savedStateHandle["walletId"]
        get() {
            return if (field == DEFAULT_WALLET_ID) null else field
        }

    init {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .zip(getAssistedWalletsFlowUseCase(Unit)) { wallets, assistedWallets ->
                    wallets to assistedWallets.getOrDefault(emptyList())
                }
                .collect { (wallets, assistedWallets) ->
                    val filterOutDeactivatedWallets = wallets.filter { wallet ->
                        assistedWallets.none { it.localId == wallet.wallet.id }
                                || assistedWallets.find { it.localId == wallet.wallet.id }?.status != WalletStatus.REPLACED.name
                    }
                    getAddressesForWallets(filterOutDeactivatedWallets)
                }
        }
    }

    private fun getAddressesForWallets(wallets: List<WalletExtended>) {
        viewModelScope.launch {
            val addressList = wallets.map { wallet ->
                async {
                    val result = getUnusedWalletAddressUseCase(wallet.wallet.id)
                    val address = result.getOrNull()?.first()
                    if (address != null) {
                        WalletAddressUi(
                            walletName = wallet.wallet.name,
                            address = address,
                            walletId = wallet.wallet.id,
                        )
                    } else {
                        null
                    }
                }
            }.awaitAll()
            val resultWalletList = addressList.filterNotNull()
            _state.update {
                it.copy(addressWalletUis = resultWalletList)
            }
            var foundWalletId = false
            if (preWalletId.isNullOrEmpty().not()) {
                resultWalletList.find { it.walletId == preWalletId }
                    .also { walletAddressUi ->
                        if (walletAddressUi != null) {
                            foundWalletId = true
                            _state.update {
                                it.copy(
                                    selectedWalletAddress = walletAddressUi,
                                    preSelectedWalletAddress = walletAddressUi.copy(address = preAddress.orEmpty())
                                )
                            }
                        }
                    }
            }
            if (foundWalletId.not() && preAddress.isNullOrEmpty().not()) {
                resultWalletList.filter { it.address == preAddress }.also { list ->
                    if (list.isEmpty()) {
                        _state.update {
                            it.copy(
                                enteredAddress = preAddress.orEmpty(),
                                showOtherAddress = true
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                selectedWalletAddress = list.first(),
                            )
                        }
                    }
                }
            }
            if (foundWalletId.not() && preAddress.isNullOrEmpty()) {
                _state.update {
                    it.copy(
                        selectedWalletAddress = resultWalletList.firstOrNull(),
                        showOtherAddress = false
                    )
                }
            }
        }
    }

    fun parseBtcUri(content: String) {
        viewModelScope.launch {
            val result = parseBtcUriUseCase(content)
            if (result.isSuccess) {
                val btcUri = result.getOrThrow()
                _state.update {
                    it.copy(enteredAddress = btcUri.address)
                }
            } else {
                _state.update {
                    it.copy(error = result.exceptionOrNull()?.message.orUnknownError())
                }
            }
        }
    }

    fun checkAddressValid(address: String) {
        viewModelScope.launch {
            checkAddressValidUseCase(CheckAddressValidUseCase.Params(listOf(address)))
                .onSuccess { result ->
                    if (result.isNotEmpty()) {
                        _state.update {
                            it.copy(isAddressInvalid = true)
                        }
                    } else {
                        _state.update {
                            it.copy(isAddressInvalid = false, checkAddressSuccess = true)
                        }
                    }
                }
        }
    }

    fun consumeCheckAddressSuccess() {
        _state.update {
            it.copy(checkAddressSuccess = false)
        }
    }

    fun updateEnteredAddress(address: String) {
        _state.update {
            it.copy(enteredAddress = address, isAddressInvalid = false)
        }
    }

    fun updateShowOtherAddress(showOtherAddress: Boolean) {
        _state.update {
            it.copy(showOtherAddress = showOtherAddress)
        }
    }

    fun getWalletIdByAddress(address: String): String? {
        return state.value.addressWalletUis.find { it.address == address }?.walletId
    }

    fun updateSelectedWalletAddress(walletAddressUi: WalletAddressUi?) {
        _state.update {
            it.copy(
                selectedWalletAddress = walletAddressUi,
            )
        }
    }

    fun isEnableButton(): Boolean {
        val stateValue = state.value
        if (stateValue.showOtherAddress) {
            return stateValue.enteredAddress.isNotBlank() && stateValue.enteredAddress != preAddress
        } else {
            if (stateValue.preSelectedWalletAddress != null
                && stateValue.selectedWalletAddress != null
                && stateValue.preSelectedWalletAddress.address != stateValue.selectedWalletAddress.address
            ) {
                return true
            }
            if (stateValue.selectedWalletAddress != null && stateValue.selectedWalletAddress.address != preAddress) {
                return true
            }
        }
        return false
    }
}

data class ReferralAddressUiState(
    val addressWalletUis: List<WalletAddressUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val enteredAddress: String = "",
    val isAddressInvalid: Boolean = false,
    val checkAddressSuccess: Boolean = false,
    val showOtherAddress: Boolean = false,
    val selectedWalletAddress: WalletAddressUi? = null,
    val preSelectedWalletAddress: WalletAddressUi? = null,
)

data class WalletAddressUi(
    val walletName: String,
    val address: String,
    val walletId: String,
)