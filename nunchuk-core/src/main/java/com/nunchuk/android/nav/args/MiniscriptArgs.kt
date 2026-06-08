package com.nunchuk.android.nav.args

import android.os.Bundle
import android.os.Parcelable
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.type.AddressType
import kotlinx.parcelize.Parcelize

@Parcelize
data class MiniscriptArgs(
    val walletName: String,
    val addressType: AddressType,
    val fromAddWallet: Boolean = false,
    val multisignType: MultisignType = MultisignType.CUSTOM,
    val template: String = "",
    val replaceWalletId: String = "",
    val isReplaceKeyWithTimelock: Boolean = false
) : Parcelable {
    fun buildBundle() = Bundle().apply {
        putString(WALLET_NAME, walletName)
        putInt(ADDRESS_TYPE, addressType.ordinal)
        putBoolean(FROM_ADD_WALLET, fromAddWallet)
        putInt(MULTISIGN_TYPE, multisignType.ordinal)
        putString(TEMPLATE, template)
        putString(REPLACE_WALLET_ID, replaceWalletId)
        putBoolean(IS_REPLACE_KEY_WITH_TIMELOCK, isReplaceKeyWithTimelock)
    }

    companion object {
        private const val WALLET_NAME = "WALLET_NAME"
        private const val ADDRESS_TYPE = "ADDRESS_TYPE"
        private const val FROM_ADD_WALLET = "FROM_ADD_WALLET"
        private const val MULTISIGN_TYPE = "MULTISIGN_TYPE"
        private const val TEMPLATE = "TEMPLATE"
        private const val REPLACE_WALLET_ID = "REPLACE_WALLET_ID"
        private const val IS_REPLACE_KEY_WITH_TIMELOCK = "IS_REPLACE_KEY_WITH_TIMELOCK"

        fun deserializeFrom(bundle: Bundle): MiniscriptArgs = MiniscriptArgs(
            walletName = bundle.getString(WALLET_NAME, "") ?: "",
            addressType = bundle.getInt(ADDRESS_TYPE, AddressType.ANY.ordinal)
                .let { AddressType.entries[it] },
            fromAddWallet = bundle.getBoolean(FROM_ADD_WALLET, false),
            multisignType = bundle.getInt(MULTISIGN_TYPE, MultisignType.CUSTOM.ordinal)
                .let { MultisignType.entries[it] },
            template = bundle.getString(TEMPLATE, "") ?: "",
            replaceWalletId = bundle.getString(REPLACE_WALLET_ID, "") ?: "",
            isReplaceKeyWithTimelock = bundle.getBoolean(IS_REPLACE_KEY_WITH_TIMELOCK, false)
        )
    }
}