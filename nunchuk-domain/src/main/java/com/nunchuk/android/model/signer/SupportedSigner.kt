package com.nunchuk.android.model.signer

import android.os.Parcelable
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.type.WalletType
import kotlinx.parcelize.Parcelize

@Parcelize
data class SupportedSigner(
    val type: SignerType,
    val tag: SignerTag?,
    val walletType: WalletType?,
    val addressType: AddressType?
) : Parcelable

val supportedSeverSigner = SupportedSigner(
    type = SignerType.SERVER,
    tag = null,
    walletType = null,
    addressType = null
)

val supportedAirgapSigner = SupportedSigner(
    type = SignerType.AIRGAP,
    tag = null,
    walletType = null,
    addressType = null
)
/**
 * Whether this supported-signer row covers a key of [type] with [tags]: the type has to match,
 * the tag only when the row names one, and the wallet type only when both sides name one (a row
 * without a wallet type covers every wallet type; a null [walletType] skips the check).
 */
fun SupportedSigner.matches(type: SignerType, tags: List<SignerTag>, walletType: WalletType? = null): Boolean =
    this.type == type &&
            (tag == null || tags.contains(tag)) &&
            (walletType == null || this.walletType == null || this.walletType == walletType)
