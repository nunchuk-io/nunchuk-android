/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *							          *
 * Copyright (C) 2022 Nunchuk								              *
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

package com.nunchuk.android.app.provider

import android.content.Context
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.components.detail.RoomDetailArgs
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import javax.inject.Inject

class PushNotificationIntentProviderImpl @Inject constructor(
    private val context: Context
) : PushNotificationIntentProvider {

    override fun getRoomDetailsIntent(roomId: String) = RoomDetailArgs(roomId).buildIntent(context)

    override fun getMainIntent() = MainActivity.createIntent(context)
}