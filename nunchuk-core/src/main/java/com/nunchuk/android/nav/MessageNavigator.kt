package com.nunchuk.android.nav

import android.content.Context
import androidx.fragment.app.FragmentManager

interface MessageNavigator {

    fun openRoomDetailActivity(activityContext: Context, roomId: String)

    fun openAddContactsScreen(fragmentManager: FragmentManager)

    fun openCreateRoomScreen(fragmentManager: FragmentManager)

}