package com.nunchuk.android.core.qr

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class DynamicQRCodeArgs(val values: List<String>) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, DynamicQRCodeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_VALUES, values.joinToString(separator = ","))
    }

    companion object {
        private const val EXTRA_WALLET_VALUES = "EXTRA_WALLET_VALUES"

        fun deserializeFrom(intent: Intent): DynamicQRCodeArgs = DynamicQRCodeArgs(
            intent.extras?.getString(EXTRA_WALLET_VALUES, "").orEmpty().split(",")
        )
    }
}