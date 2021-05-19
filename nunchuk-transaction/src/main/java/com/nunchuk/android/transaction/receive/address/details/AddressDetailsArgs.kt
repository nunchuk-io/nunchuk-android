package com.nunchuk.android.transaction.receive.address.details

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getLongValue
import com.nunchuk.android.core.util.getStringValue

data class AddressDetailsArgs(
    val address: String,
    val balance: Long = 0L
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(
        activityContext,
        AddressDetailsActivity::class.java
    ).apply {
        putExtra(EXTRA_ADDRESS, address)
        putExtra(EXTRA_BALANCE, balance)
    }

    companion object {
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_BALANCE = "EXTRA_BALANCE"

        fun deserializeFrom(intent: Intent): AddressDetailsArgs = AddressDetailsArgs(
            intent.extras.getStringValue(EXTRA_ADDRESS),
            intent.extras.getLongValue(EXTRA_BALANCE)
        )
    }
}