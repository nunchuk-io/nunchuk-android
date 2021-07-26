package com.nunchuk.android.messages.components.group

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class ChatGroupInfoArgs(val roomId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ChatGroupInfoActivity::class.java).apply {
        putExtra(EXTRA_ROOM_ID, roomId)
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        fun deserializeFrom(intent: Intent) = ChatGroupInfoArgs(
            intent.extras?.getString(EXTRA_ROOM_ID, "").orEmpty(),
        )
    }

}