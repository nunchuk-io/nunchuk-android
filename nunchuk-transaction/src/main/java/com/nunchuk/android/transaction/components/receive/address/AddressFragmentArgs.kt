package com.nunchuk.android.transaction.components.receive.address

import android.os.Bundle
import com.nunchuk.android.arch.args.FragmentArgs
import com.nunchuk.android.core.util.getStringValue

data class AddressFragmentArgs(val walletId: String) : FragmentArgs {

    override fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_ID, walletId)
    }

    companion object {

        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"

        fun deserializeFrom(data: Bundle?) = AddressFragmentArgs(data.getStringValue(EXTRA_WALLET_ID))

    }
}
