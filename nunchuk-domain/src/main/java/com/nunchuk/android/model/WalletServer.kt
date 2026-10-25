package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.model.inheritance.OnChainInheritancePolicy
import com.nunchuk.android.model.signer.SignerServer
import com.nunchuk.android.type.WalletType
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletTimelock(
    val timelockValue: Long = 0L,
    val timezone: String = "",
    val based: TimelockBased = TimelockBased.TIME_LOCK,
    val blockHeight: Long? = null
) : Parcelable

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
    val timelock: WalletTimelock = WalletTimelock(),
    /** On-chain only; null when the server sends none. */
    val onChainInheritancePolicy: OnChainInheritancePolicy? = null,
)
/** The same lock in the shape the timelock screens and membership steps use. */
fun WalletTimelock.toTimelockExtra() = TimelockExtra(
    value = timelockValue,
    timezone = timezone,
    based = based,
    blockHeight = blockHeight,
)
