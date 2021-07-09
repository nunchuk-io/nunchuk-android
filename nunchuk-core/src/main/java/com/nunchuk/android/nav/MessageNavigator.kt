package com.nunchuk.android.nav

import android.content.Context

interface MessageNavigator {

    fun openRoomDetailActivity(activityContext: Context, roomId: String)

}