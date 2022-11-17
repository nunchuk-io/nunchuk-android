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

package com.nunchuk.android.wallet.shared.components.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.RoomWalletData

data class SharedWalletConfigArgs(
    val roomWalletData: RoomWalletData?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context): Intent {
        return Intent(activityContext, SharedWalletConfigActivity::class.java).apply {
            putExtras(Bundle().apply {
                putParcelable(EXTRA_ROOM_WALLET_DATA, roomWalletData)
            })
        }
    }

    companion object {
        private const val EXTRA_ROOM_WALLET_DATA = "EXTRA_ROOM_WALLET_DATA"

        fun deserializeFrom(intent: Intent) = SharedWalletConfigArgs(
            intent.extras?.getParcelable(EXTRA_ROOM_WALLET_DATA)
        )
    }
}