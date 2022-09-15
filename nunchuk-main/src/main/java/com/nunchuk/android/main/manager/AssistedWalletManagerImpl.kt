package com.nunchuk.android.main.manager

import com.nunchuk.android.core.domain.GetAssistedWalletIdsFlowUseCase
import com.nunchuk.android.manager.AssistedWalletManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal class AssistedWalletManagerImpl @Inject constructor(
    getAssistedWalletIdsFlowUseCase: GetAssistedWalletIdsFlowUseCase,
    applicationScope: CoroutineScope,
) : AssistedWalletManager {
    override val assistedWalletIds =
        getAssistedWalletIdsFlowUseCase(Unit)
            .map { it.getOrElse { emptySet() } }
            .stateIn(applicationScope, SharingStarted.Eagerly, emptySet())

    override fun isAssistedWallet(walletId: String): Boolean {
       return assistedWalletIds.value.contains(walletId)
    }
}