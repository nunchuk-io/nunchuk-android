package com.nunchuk.android.messages.nav

import android.content.Context
import com.nunchuk.android.messages.room.detail.RoomDetailActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(activityContext: Context, roomId: String) {
        RoomDetailActivity.start(activityContext, roomId)
    }

}