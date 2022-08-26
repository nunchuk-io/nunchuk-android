package com.nunchuk.android.signer.software.components.primarykey.notification

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.signer.PrimaryKeyFlow

data class PKeyNotificationArgs(
    val messages: ArrayList<String>,
    val primaryKeyFlow: Int
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyNotificationActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE_LIST, messages)
            putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
        }

    companion object {
        private const val EXTRA_MESSAGE_LIST = "EXTRA_MESSAGE_LIST"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"

        fun deserializeFrom(intent: Intent) = PKeyNotificationArgs(
            intent.extras?.getStringArrayList(EXTRA_MESSAGE_LIST).orEmpty() as ArrayList<String>,
            intent.extras?.getInt(EXTRA_PRIMARY_KEY_FLOW) ?: PrimaryKeyFlow.NONE,
        )
    }
}