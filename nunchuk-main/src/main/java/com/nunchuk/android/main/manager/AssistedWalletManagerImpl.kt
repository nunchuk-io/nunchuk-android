package com.nunchuk.android.main.manager

import com.nunchuk.android.core.domain.GetAssistedWalletIdFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class AssistedWalletManagerImpl @Inject constructor(
    getAssistedWalletIdFlowUseCase: GetAssistedWalletIdFlowUseCase,
    applicationScope: CoroutineScope,
) : AssistedWalletManager {
    private val _assistedWalletId = getAssistedWalletIdFlowUseCase(Unit)
        .map { it.getOrNull().orEmpty() }
        .stateIn(applicationScope, SharingStarted.Eagerly, "")
    override val assistedWalletId: Flow<String> = _assistedWalletId

    override fun isAssistedWallet(walletId: String): Boolean {
        return _assistedWalletId.value == walletId
    }
}