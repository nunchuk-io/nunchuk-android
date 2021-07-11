package com.nunchuk.android.messages.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.messages.contact.AddContactsBottomSheet
import com.nunchuk.android.messages.room.detail.RoomDetailActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(activityContext: Context, roomId: String) {
        RoomDetailActivity.start(activityContext, roomId)
    }

    override fun openAddContactsScreen(fragmentManager: FragmentManager) {
        AddContactsBottomSheet.show(fragmentManager)
    }

}