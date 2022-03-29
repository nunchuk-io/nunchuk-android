package com.nunchuk.android.messages.components.group.members

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class GroupMembersArgs(val roomId: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, GroupMembersActivity::class.java).apply {
        putExtra(EXTRA_ROOM_ID, roomId)
    }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"

        fun deserializeFrom(intent: Intent) = GroupMembersArgs(
            intent.extras?.getString(EXTRA_ROOM_ID, "").orEmpty(),
        )
    }

}
