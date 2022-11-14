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

package com.nunchuk.android.auth.components.recover

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

internal data class RecoverPasswordArgs(val email: String) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, RecoverPasswordActivity::class.java).apply {
        putExtra(EXTRA_EMAIL_ADDRESS, email)
    }

    companion object {
        private const val EXTRA_EMAIL_ADDRESS = "EXTRA_EMAIL_ADDRESS"

        fun deserializeFrom(intent: Intent) = RecoverPasswordArgs(intent.extras?.getString(EXTRA_EMAIL_ADDRESS, "").orEmpty())
    }

}