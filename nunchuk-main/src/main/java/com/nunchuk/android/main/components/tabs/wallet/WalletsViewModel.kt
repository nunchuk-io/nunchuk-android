package com.nunchuk.android.main.components.tabs.wallet

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
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

    fun handleAddSignerOrWallet() {
        if (hasSigner()) {
            handleAddWallet()
        } else {
            handleAddSigner()
        }
    }

    fun handleAddSigner() {
        if (hasSigner()) {
            event(AddSignerEvent)
        } else {
            event(ShowSignerIntroEvent)
        }
    }

    fun handleAddWallet() {
        if (hasSigner()) {
            event(AddWalletEvent)
        } else {
            event(ShowErrorEvent("You need to add signer before creating wallet"))
        }
    }

    private fun hasSigner() = walletsState.signers.isNotEmpty()

}