package com.nunchuk.android.transaction.components.send.fee

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue

data class EstimatedFeeArgs(
    val walletId: String,
    val outputAmount: Double,
    val availableAmount: Double,
    val address: String,
    val privateNote: String,
    val subtractFeeFromAmount: Boolean = false
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, EstimatedFeeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putExtra(EXTRA_ADDRESS, address)
        putExtra(EXTRA_PRIVATE_NOTE, privateNote)
        putExtra(EXTRA_SUBTRACT_FEE, subtractFeeFromAmount)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        private const val EXTRA_PRIVATE_NOTE = "EXTRA_PRIVATE_NOTE"
        private const val EXTRA_SUBTRACT_FEE = "EXTRA_SUBTRACT_FEE"

        fun deserializeFrom(intent: Intent) = EstimatedFeeArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            intent.extras.getStringValue(EXTRA_ADDRESS),
            intent.extras.getStringValue(EXTRA_PRIVATE_NOTE),
            intent.extras.getBooleanValue(EXTRA_SUBTRACT_FEE)
        )
    }
}