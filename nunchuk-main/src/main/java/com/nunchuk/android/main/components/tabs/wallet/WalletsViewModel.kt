package com.nunchuk.android.main.components.tabs.wallet

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.usecase.GetMasterSignersUseCase
import com.nunchuk.android.usecase.GetRemoteSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getMasterSignersUseCase: GetMasterSignersUseCase,
    private val getRemoteSignersUseCase: GetRemoteSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    fun retrieveData() {
        getSigners()
        getWallets()
    }

    private fun getSigners() {
        getMasterSigners()
        getRemoteSigners()
    }

    private fun getRemoteSigners() {
        viewModelScope.launch {
            getRemoteSignersUseCase.execute()
                .catch {
                    updateState { copy(signers = emptyList()) }
                    Log.e(TAG, "get signers error: ${it.message}")
                }
                .collect { updateState { copy(signers = it) } }
        }
    }

    private fun getMasterSigners() {
        viewModelScope.launch {
            getMasterSignersUseCase.execute()
                .catch {
                    updateState { copy(signers = emptyList()) }
                    Log.e(TAG, "get signers error: ${it.message}")
                }
                .collect { updateState { copy(masterSigners = it) } }
        }
    }

    private fun getWallets() {
        viewModelScope.launch {
            getWalletsUseCase.execute()
                .catch {
                    updateState { copy(wallets = emptyList()) }
                    Log.e(TAG, "get wallets error: ${it.message}")
                }
                .collect { updateState { copy(wallets = it) } }
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