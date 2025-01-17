/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.messages.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.messages.components.create.CreateRoomBottomSheet
import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.messages.components.direct.ChatInfoActivity
import com.nunchuk.android.messages.components.freegroup.FreeGroupWalletChatActivity
import com.nunchuk.android.messages.components.group.ChatGroupInfoActivity
import com.nunchuk.android.nav.MessageNavigator

interface MessageNavigatorDelegate : MessageNavigator {

    override fun openRoomDetailActivity(
        activityContext: Context,
        roomId: String,
        roomAction: RoomAction,
        isGroupChat: Boolean
    ) {
        RoomDetailActivity.start(activityContext, roomId, roomAction, isGroupChat)
    }

    override fun returnRoomDetailScreen() {
        ActivityManager.popUntil(RoomDetailActivity::class.java)
    }

    override fun openCreateRoomScreen(fragmentManager: FragmentManager) {
        CreateRoomBottomSheet.show(fragmentManager)
    }

    override fun openChatInfoScreen(activityContext: Context, roomId: String, isByzantineChat: Boolean, isShowCollaborativeWallet: Boolean) {
        ChatInfoActivity.start(activityContext, roomId, isByzantineChat, isShowCollaborativeWallet)
    }

    override fun openChatGroupInfoScreen(activityContext: Context, roomId: String, isByzantineChat: Boolean, isShowCollaborativeWallet: Boolean) {
        ChatGroupInfoActivity.start(activityContext, roomId, isByzantineChat, isShowCollaborativeWallet)
    }

    override fun openGroupChatScreen(activityContext: Context, walletId: String) {
        FreeGroupWalletChatActivity.start(activityContext, walletId)
    }

}