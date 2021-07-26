package com.nunchuk.android.messages.components.direct

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class ChatInfoArgs(val roomId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ChatInfoActivity::class.java).apply {
        putExtra(EXTRA_ROOM_ID, roomId)
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        fun deserializeFrom(intent: Intent) = ChatInfoArgs(
            intent.extras?.getString(EXTRA_ROOM_ID, "").orEmpty(),
        )
    }

}