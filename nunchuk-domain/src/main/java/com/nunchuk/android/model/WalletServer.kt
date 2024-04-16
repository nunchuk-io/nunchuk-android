package com.nunchuk.android.model

import com.nunchuk.android.model.signer.SignerServer

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
)