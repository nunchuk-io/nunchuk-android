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
import android.content.Intent
import com.nunchuk.android.core.constants.RoomAction
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.messages.components.detail.RoomDetailActivity
import com.nunchuk.android.messages.components.detail.RoomDetailFragmentArgs
import com.nunchuk.android.notifications.PushNotificationIntentProvider
import com.nunchuk.android.transaction.components.details.TransactionDetailsActivity
import javax.inject.Inject

class PushNotificationIntentProviderImpl @Inject constructor(
    private val context: Context
) : PushNotificationIntentProvider {

    override fun getRoomDetailsIntent(roomId: String) = Intent(context, RoomDetailActivity::class.java).apply {
        putExtras(RoomDetailFragmentArgs(roomId, RoomAction.NONE).toBundle())
    }

    override fun getMainIntent() = MainActivity.createIntent(context)

    override fun getTransactionDetailIntent(walletId: String, txId: String): Intent {
        return TransactionDetailsActivity.buildIntent(context, walletId = walletId, txId = txId)
    }
}