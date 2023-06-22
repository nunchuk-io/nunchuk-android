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

package com.nunchuk.android.nav

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.nunchuk.android.core.constants.RoomAction

interface MessageNavigator {

    fun openRoomDetailActivity(activityContext: Context, roomId: String, roomAction: RoomAction = RoomAction.NONE)

    fun returnRoomDetailScreen()

    fun openCreateRoomScreen(fragmentManager: FragmentManager)

    fun openChatInfoScreen(activityContext: Context, roomId: String)

    fun openChatGroupInfoScreen(activityContext: Context, roomId: String)
}