package com.nunchuk.android.signer.satscard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.SatsCardStatus

data class SatsCardArgs(val status: SatsCardStatus) : ActivityArgs {
    override fun buildIntent(activityContext: Context): Intent {
        return Intent(activityContext, SatsCardActivity::class.java).apply {
            putExtra(EXTRA_SATSCARD_STATUS, status)
        }
    }

    companion object {
        const val EXTRA_SATSCARD_STATUS = "extra_satscard_status"

        fun deserializeBundle(arguments: Bundle) = SatsCardArgs(
            arguments.getParcelable(EXTRA_SATSCARD_STATUS)!!
        )
    }
}