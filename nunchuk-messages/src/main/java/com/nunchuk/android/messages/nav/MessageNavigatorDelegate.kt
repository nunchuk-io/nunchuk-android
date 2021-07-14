package com.nunchuk.android.messages.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.messages.contact.AddContactsBottomSheet
import com.nunchuk.android.messages.pending.PendingContactsBottomSheet
import com.nunchuk.android.messages.room.create.CreateRoomBottomSheet
import com.nunchuk.android.messages.room.detail.RoomDetailActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(activityContext: Context, roomId: String) {
        RoomDetailActivity.start(activityContext, roomId)
    }

    override fun openAddContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit) {
        val bottomSheet = AddContactsBottomSheet.show(fragmentManager)
        bottomSheet.listener = onDismiss
    }

    override fun openCreateRoomScreen(fragmentManager: FragmentManager) {
        CreateRoomBottomSheet.show(fragmentManager)
    }

    override fun openPendingContactsScreen(fragmentManager: FragmentManager, onDismiss: () -> Unit) {
        val bottomSheet = PendingContactsBottomSheet.show(fragmentManager)
        bottomSheet.listener = onDismiss
    }

}