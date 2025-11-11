package com.nunchuk.android.model

import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.WalletType

data class WalletServer(
    val id: String,
    val localId: String,
    val name: String,
    val description: String,
    val bsms: String,
    val slug: String,
    val serverKey: ServerKey?,
    val signers: List<SignerServer> = emptyList(),
    val status: String,
    val createdTimeMilis: Long = 0L,
    val primaryMembershipId: String,
    val alias: String,
    val walletType: WalletType = WalletType.MULTI_SIG,
    val sendBsmsEmail: Boolean = false,
    val requiresRegistration: Boolean = false,
    val timelockValue: Long = 0L,
    val timezone: String = "",
)