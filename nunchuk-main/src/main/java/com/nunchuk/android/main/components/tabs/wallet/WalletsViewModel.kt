package com.nunchuk.android.main.components.tabs.wallet

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.model.Result.Success
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    private var walletsState = WalletsState()

    override val initialState = WalletsState()

    fun retrieveData() {
        getSigners()
        getWallets()
    }

    private fun getSigners() {
        viewModelScope.launch {
            val result = getRemoteSignersUseCase.execute()
            walletsState = walletsState.copy(signers = if (result is Success) result.data else emptyList())
            updateState { walletsState }
        }
    }

    private fun getWallets() {
        viewModelScope.launch {
            val result = getWalletsUseCase.execute()
            walletsState = walletsState.copy(wallets = if (result is Success) result.data else emptyList())
            updateState { walletsState }
        }
    }

}