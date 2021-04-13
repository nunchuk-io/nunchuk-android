package com.nunchuk.android.wallet.confirm

import android.os.Parcelable
import com.nunchuk.android.model.SingleSigner
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ParcelizeSingleSigner(
    var name: String = "",
    var xpub: String = "",
    var publicKey: String = "",
    var derivationPath: String = "",
    var masterFingerprint: String = "",
    var lastHealthCheck: Long = 0L,
    var masterSignerId: String = "",
    var used: Boolean = false
) : Parcelable

internal fun List<SingleSigner>.parcelize() = map(SingleSigner::parcelize) as ArrayList<ParcelizeSingleSigner>

internal fun List<ParcelizeSingleSigner>.deparcelize() = map(ParcelizeSingleSigner::deparcelize)

internal fun SingleSigner.parcelize() = ParcelizeSingleSigner(
    name = name,
    xpub = xpub,
    publicKey = publicKey,
    derivationPath = derivationPath,
    masterFingerprint = masterFingerprint,
    lastHealthCheck = lastHealthCheck,
    masterSignerId = masterSignerId,
    used = used
)

internal fun ParcelizeSingleSigner.deparcelize() = SingleSigner(
    name = name,
    xpub = xpub,
    publicKey = publicKey,
    derivationPath = derivationPath,
    masterFingerprint = masterFingerprint,
    lastHealthCheck = lastHealthCheck,
    masterSignerId = masterSignerId,
    used = used
)
