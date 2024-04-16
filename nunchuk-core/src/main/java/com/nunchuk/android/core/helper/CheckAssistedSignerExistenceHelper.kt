package com.nunchuk.android.core.helper

import com.nunchuk.android.core.domain.GetAssistedWalletsFlowUseCase
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.membership.AssistedWalletBrief
import com.nunchuk.android.usecase.GetWalletsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class CheckAssistedSignerExistenceHelper @Inject constructor(
    private val getAssistedWalletsFlowUseCase: GetAssistedWalletsFlowUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
) {

    private var assistedWallets: List<AssistedWalletBrief> = emptyList()
    private var wallets: List<WalletExtended> = emptyList()

    fun init(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            getAssistedWalletsFlowUseCase(Unit).map { it.getOrElse { emptyList() } }
                .distinctUntilChanged()
                .collect {
                    this@CheckAssistedSignerExistenceHelper.assistedWallets = it
                }
        }
        coroutineScope.launch {
            getWalletsUseCase.execute()
                .distinctUntilChanged()
                .collect {
                    wallets = it
                }
        }
    }

    fun isInWallet(signer: SignerModel): Boolean {
        return checkSignerExistence(signer, wallets)
    }

    fun isInAssistedWallet(signer: SignerModel): Boolean {
        val assistedWalletSet = assistedWallets.map { it.localId }.toHashSet()
        val assistedWallets = wallets.filter { it.wallet.id in assistedWalletSet }
        return checkSignerExistence(signer, assistedWallets)
    }

    fun isInAssistedWallet(masterSignerId: String): Boolean {
        val assistedWalletSet = assistedWallets.map { it.localId }.toHashSet()
        val assistedWallets = wallets.filter { it.wallet.id in assistedWalletSet }
        return assistedWallets.any {
            it.wallet.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterSignerId == masterSignerId
                }
                return@anyLast false
            }
        }
    }

    private fun checkSignerExistence(
        signer: SignerModel,
        walletExtendeds: List<WalletExtended>,
    ): Boolean {
        return walletExtendeds.any {
            it.wallet.signers.any anyLast@{ singleSigner ->
                if (singleSigner.hasMasterSigner) {
                    return@anyLast singleSigner.masterFingerprint == signer.fingerPrint
                }
                return@anyLast singleSigner.masterFingerprint == signer.fingerPrint && singleSigner.derivationPath == signer.derivationPath
            }
        }
    }
}