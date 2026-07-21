package com.nunchuk.android.model.byzantine

import com.nunchuk.android.model.TimelockBased
import com.nunchuk.android.model.WalletConfig
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.WalletType

data class DraftWallet(
    val groupId: String? = null,
    val config: WalletConfig,
    val isMasterSecurityQuestionSet: Boolean,
    val signers: List<SignerServer>,
    val walletType: WalletType?,
    val timelock: DraftWalletTimelock = DraftWalletTimelock(),
    val replaceWallet: ReplaceWallet? = null,
    val miniscriptTemplate: String? = null,
    val platformKeySlots: List<String> = emptyList(),
    val addressType: String? = null,
    val walletTemplate: String? = null,
    val isCustomized: Boolean = false,
)

data class DraftWalletTimelock(
    val value: Long = 0L,
    val timezone: String = "",
    val based: TimelockBased = TimelockBased.TIME_LOCK,
    val blockHeight: Long? = null
)