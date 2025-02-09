package com.nunchuk.android.core.data.model

import android.os.Parcelable
import com.nunchuk.android.type.AddressType
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletConfigViewOnlyDataComposer(
    val walletName: String,
    val addressType: AddressType,
    val requireKeys: Int,
    val totalKeys: Int,
    val walletConfigType: WalletConfigType
) : Parcelable