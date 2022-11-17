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

package com.nunchuk.android.signer.software.components.primarykey.notification

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.signer.PrimaryKeyFlow

data class PKeyNotificationArgs(
    val messages: ArrayList<String>,
    val primaryKeyFlow: Int
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyNotificationActivity::class.java).apply {
            putExtra(EXTRA_MESSAGE_LIST, messages)
            putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
        }

    companion object {
        private const val EXTRA_MESSAGE_LIST = "EXTRA_MESSAGE_LIST"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"

        fun deserializeFrom(intent: Intent) = PKeyNotificationArgs(
            intent.extras?.getStringArrayList(EXTRA_MESSAGE_LIST).orEmpty() as ArrayList<String>,
            intent.extras?.getInt(EXTRA_PRIMARY_KEY_FLOW) ?: PrimaryKeyFlow.NONE,
        )
    }
}