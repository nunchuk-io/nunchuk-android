package com.nunchuk.android.core.wallet

import android.os.Parcelable
import com.nunchuk.android.model.SavedAddress
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletBottomSheetResult(
    val walletId: String? = null,
    val walletName: String? = null,
    val savedAddress: SavedAddress? = null
) : Parcelable