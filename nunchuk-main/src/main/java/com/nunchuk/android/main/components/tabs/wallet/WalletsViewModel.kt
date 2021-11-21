package com.nunchuk.android.main.components.tabs.wallet

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.domain.AddBlockChainConnectionListenerUseCase
import com.nunchuk.android.core.domain.GetAppSettingUseCase
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent.*
import com.nunchuk.android.model.ConnectionStatusExecutor
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.usecase.GetCompoundSignersUseCase
import com.nunchuk.android.usecase.GetWalletsUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class WalletsViewModel @Inject constructor(
    private val getCompoundSignersUseCase: GetCompoundSignersUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val addBlockChainConnectionListenerUseCase: AddBlockChainConnectionListenerUseCase,
    private val getAppSettingUseCase: GetAppSettingUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    fun getAppSettings() {
        viewModelScope.launch {
            getAppSettingUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    updateState {
                        copy(chain = it.chain)
                    }
                }
        }
    }

    fun registerBlockChainConnectionStatusExecutor() {
        ConnectionStatusHelper.executor = object : ConnectionStatusExecutor {
            override fun execute(connectionStatus: ConnectionStatus, percent: Int) {
                postState {
                    copy(
                        connectionStatus = connectionStatus
                    )
                }
            }
        }
    }

    fun addBlockChainConnectionListener() {
        viewModelScope.launch {
            addBlockChainConnectionListenerUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {}
        }
    }

    fun retrieveData() {
        viewModelScope.launch {
            getCompoundSignersUseCase.execute()
                .zip(getWalletsUseCase.execute()) { p, wallets ->
                    Triple(p.first, p.second, wallets)
                }
                .onStart { event(Loading(true)) }
                .flowOn(Dispatchers.IO)
                .onException {
                    updateState { copy(signers = emptyList(), masterSigners = emptyList()) }
                }
                .flowOn(Dispatchers.Main)
                .onCompletion {
                    event(Loading(false))
                }
                .collect { updateState { copy(masterSigners = it.first, signers = it.second, wallets = it.third) } }
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

}