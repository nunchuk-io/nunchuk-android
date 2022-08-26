package com.nunchuk.android.signer.software.components.primarykey.account

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.PrimaryKey

data class PKeyAccountArgs(
    val accounts: ArrayList<PrimaryKey>
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyAccountActivity::class.java).apply {
            putParcelableArrayListExtra(EXTRA_ACCOUNTS, accounts)
        }

    companion object {
        private const val EXTRA_ACCOUNTS = "EXTRA_ACCOUNTS"

        fun deserializeFrom(intent: Intent) = PKeyAccountArgs(
            intent.extras?.getParcelableArrayList<PrimaryKey>(EXTRA_ACCOUNTS)
                .orEmpty() as ArrayList<PrimaryKey>,
        )
    }
}