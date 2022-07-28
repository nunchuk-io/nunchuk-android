package com.nunchuk.android.messages.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.messages.components.create.CreateRoomBottomSheet
import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.messages.components.direct.ChatInfoActivity
import com.nunchuk.android.messages.components.group.ChatGroupInfoActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(activityContext: Context, roomId: String, roomAction: RoomAction?) {
        RoomDetailActivity.start(activityContext, roomId, roomAction)
    }

    override fun returnRoomDetailScreen() {
        ActivityManager.popUntil(RoomDetailActivity::class.java)
    }

    override fun openCreateRoomScreen(fragmentManager: FragmentManager) {
        CreateRoomBottomSheet.show(fragmentManager)
    }

    override fun openChatInfoScreen(activityContext: Context, roomId: String) {
        ChatInfoActivity.start(activityContext, roomId)
    }

    override fun openChatGroupInfoScreen(activityContext: Context, roomId: String) {
        ChatGroupInfoActivity.start(activityContext, roomId)
    }

}