package com.nunchuk.android.signer.software.components.primarykey.signin

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.PrimaryKey

data class PKeySignInArgs(
    val primaryKey: PrimaryKey
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeySignInActivity::class.java).apply {
            putExtra(EXTRA_PRIMARY_KEY, primaryKey)
        }

    companion object {
        private const val EXTRA_PRIMARY_KEY = "EXTRA_PRIMARY_KEY"

        fun deserializeFrom(intent: Intent) = PKeySignInArgs(
            intent.extras?.getParcelable(EXTRA_PRIMARY_KEY)!!
        )
    }
}