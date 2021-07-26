package com.nunchuk.android.nav

import android.content.Context
import androidx.fragment.app.FragmentManager

interface MessageNavigator {

    fun openRoomDetailActivity(activityContext: Context, roomId: String)

    fun openCreateRoomScreen(fragmentManager: FragmentManager)

    fun openChatInfoScreen(activityContext: Context, roomId: String)

    fun openChatGroupInfoScreen(activityContext: Context, roomId: String)
}