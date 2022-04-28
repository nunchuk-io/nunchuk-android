package com.nunchuk.android.messages.components.detail

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.constants.RoomAction

data class RoomDetailArgs(val roomId: String, val roomAction: RoomAction? = null) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, RoomDetailActivity::class.java).apply {
            putExtra(EXTRA_ROOM_ID, roomId)
            putExtra(EXTRA_ROOM_ACTION, roomAction)
        }

    companion object {
        private const val EXTRA_ROOM_ID = "EXTRA_ROOM_ID"
        private const val EXTRA_ROOM_ACTION = "EXTRA_ROOM_ACTION"

        fun deserializeFrom(intent: Intent) = RoomDetailArgs(
            intent.extras?.getString(EXTRA_ROOM_ID, "").orEmpty(),
            intent.extras?.getSerializable(EXTRA_ROOM_ACTION) as RoomAction?,
        )

    }

}