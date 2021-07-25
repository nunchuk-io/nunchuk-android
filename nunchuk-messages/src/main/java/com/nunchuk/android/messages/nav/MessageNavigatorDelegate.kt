package com.nunchuk.android.messages.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.messages.components.create.CreateRoomBottomSheet
import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(activityContext: Context, roomId: String) {
        RoomDetailActivity.start(activityContext, roomId)
    }

    override fun openCreateRoomScreen(fragmentManager: FragmentManager) {
        CreateRoomBottomSheet.show(fragmentManager)
    }


}