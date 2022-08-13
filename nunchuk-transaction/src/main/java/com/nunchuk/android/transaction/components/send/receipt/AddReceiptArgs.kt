package com.nunchuk.android.transaction.components.send.receipt

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.getBooleanValue
import com.nunchuk.android.core.util.getDoubleValue
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.SatsCardSlot

data class AddReceiptArgs(
    val walletId: String,
    val outputAmount: Double,
    val availableAmount: Double,
    val subtractFeeFromAmount: Boolean = false,
    val slots: List<SatsCardSlot>,
    val sweepType: SweepType
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, AddReceiptActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_OUTPUT_AMOUNT, outputAmount)
        putExtra(EXTRA_AVAILABLE_AMOUNT, availableAmount)
        putExtra(EXTRA_SUBTRACT_FEE, subtractFeeFromAmount)
        putExtra(EXTRA_SWEEP_TYPE, sweepType)
        putParcelableArrayListExtra(EXTRA_SLOTS, ArrayList(slots))
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_OUTPUT_AMOUNT = "EXTRA_OUTPUT_AMOUNT"
        private const val EXTRA_AVAILABLE_AMOUNT = "EXTRA_AVAILABLE_AMOUNT"
        private const val EXTRA_SUBTRACT_FEE = "EXTRA_SUBTRACT_FEE"
        private const val EXTRA_SLOTS = "EXTRA_SLOTS"
        private const val EXTRA_SWEEP_TYPE = "EXTRA_SWEEP_TYPE"

        fun deserializeFrom(intent: Intent) = AddReceiptArgs(
            intent.extras.getStringValue(EXTRA_WALLET_ID),
            intent.extras.getDoubleValue(EXTRA_OUTPUT_AMOUNT),
            intent.extras.getDoubleValue(EXTRA_AVAILABLE_AMOUNT),
            intent.extras.getBooleanValue(EXTRA_SUBTRACT_FEE),
            intent.extras!!.getParcelableArrayList<SatsCardSlot>(EXTRA_SLOTS).orEmpty(),
            intent.extras!!.getSerializable(EXTRA_SWEEP_TYPE) as SweepType
        )
    }
}