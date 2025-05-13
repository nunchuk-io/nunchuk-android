package com.nunchuk.android.nav.args

import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.type.AddressType
import kotlinx.parcelize.Parcelize

@Parcelize
data class MiniscriptArgs(
    val walletName: String,
    val addressType: AddressType
) : Parcelable {
    fun buildBundle() = Bundle().apply {
        putString(WALLET_NAME, walletName)
        putInt(ADDRESS_TYPE, addressType.ordinal)
    }

    companion object {
        private const val WALLET_NAME = "WALLET_NAME"
        private const val ADDRESS_TYPE = "ADDRESS_TYPE"

        fun deserializeFrom(bundle: Bundle): MiniscriptArgs = MiniscriptArgs(
            walletName = bundle.getString(WALLET_NAME, "") ?: "",
            addressType = bundle.getInt(ADDRESS_TYPE, AddressType.ANY.ordinal)
                .let { AddressType.entries[it] }
        )
    }
}