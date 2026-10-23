package com.nunchuk.android.model.wallet

import com.nunchuk.android.model.WalletTimelock
import com.nunchuk.android.model.inheritance.DraftInheritanceCard
import com.nunchuk.android.model.inheritance.InheritanceValidationError
import com.nunchuk.android.model.signer.SignerServer

/**
 * @property inheritanceCards configurable on-chain wallets (NUN-10320): the wallet's cards in the
 *   server's order. Join [DraftInheritanceCard.keys] to the wallet signers and to [replacements] by
 *   key index. Empty for a legacy wallet.
 * @property validationErrors issues of the pending replacement, shown before it can be finalized.
 * @property timelock the effective primary time (pending change included).
 * @property fallbackTimelocks the effective fallback times; index 0 is fallback 0.
 */
class ReplaceWalletStatus(
    val pendingReplaceXfps: List<String> = emptyList(),
    val signers: Map<String, SignerServer> = emptyMap(),
    val replacements: Map<String, List<SignerServer>> = emptyMap(),
    val timelock: WalletTimelock? = null,
    val inheritanceCards: List<DraftInheritanceCard> = emptyList(),
    val validationErrors: List<InheritanceValidationError> = emptyList(),
    val fallbackTimelocks: List<WalletTimelock> = emptyList(),
)
