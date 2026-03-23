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