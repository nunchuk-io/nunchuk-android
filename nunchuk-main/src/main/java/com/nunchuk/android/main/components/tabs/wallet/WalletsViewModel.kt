package com.nunchuk.android.main.components.tabs.wallet

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    fun retrieveData() {
        getWallets()
        getSigners()
    }

    private fun getSigners() {
        getCompoundSignersUseCase.execute()
            .onStart { event(SignersLoading(true)) }
            .flowOn(Dispatchers.IO)
            .catch {
                updateState { copy(signers = emptyList(), masterSigners = emptyList()) }
                Log.e(TAG, "get signers error: ${it.message}")
            }
            .flowOn(Dispatchers.Main)
            .onCompletion { event(SignersLoading(false)) }
            .onEach { updateState { copy(masterSigners = it.first, signers = it.second) } }
            .launchIn(viewModelScope)
    }

    private fun getWallets() {
        getWalletsUseCase.execute()
            .onStart { event(WalletLoading(true)) }
            .flowOn(Dispatchers.IO)
            .catch {
                updateState { copy(wallets = emptyList()) }
                Log.e(TAG, "get wallets error: ${it.message}")
            }
            .flowOn(Dispatchers.Main)
            .onCompletion { event(WalletLoading(false)) }
            .onEach { updateState { copy(wallets = it) } }
            .launchIn(viewModelScope)
    }

    fun handleAddSignerOrWallet() {
        if (hasSigner()) {
            handleAddWallet()
        } else {
            handleAddSigner()
        }
    }

    fun handleAddSigner() {
        event(ShowSignerIntroEvent)
    }

    fun handleAddWallet() {
        if (hasSigner()) {
            event(AddWalletEvent)
        } else {
            event(WalletEmptySignerEvent)
        }
    }

    private fun hasSigner() = getState().signers.isNotEmpty() || getState().masterSigners.isNotEmpty()

    companion object {
        private const val TAG = "WalletsViewModel"
    }
}