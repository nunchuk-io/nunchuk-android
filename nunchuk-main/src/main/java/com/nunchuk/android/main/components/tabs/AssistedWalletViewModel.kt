package com.nunchuk.android.main.components.tabs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.usecase.GetWalletsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AssistedWalletViewModel @Inject constructor(
    private val getWalletsUseCase: GetWalletsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<List<WalletExtended>>(emptyList())
    val state = _state.asStateFlow()

    fun loadWallets(assistedWalletIds: List<String>) {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch { Timber.e(it) }
                .collect { wallets ->
                    _state.value = wallets.filter { it.wallet.id in assistedWalletIds }
                }
        }
    }
}