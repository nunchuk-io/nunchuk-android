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

package com.nunchuk.android.signer.software.components.primarykey.signin

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.PrimaryKey

data class PKeySignInArgs(
    val primaryKey: PrimaryKey
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeySignInActivity::class.java).apply {
            putExtra(EXTRA_PRIMARY_KEY, primaryKey)
        }

    companion object {
        private const val EXTRA_PRIMARY_KEY = "EXTRA_PRIMARY_KEY"

        fun deserializeFrom(intent: Intent) = PKeySignInArgs(
            intent.extras?.getParcelable(EXTRA_PRIMARY_KEY)!!
        )
    }
}