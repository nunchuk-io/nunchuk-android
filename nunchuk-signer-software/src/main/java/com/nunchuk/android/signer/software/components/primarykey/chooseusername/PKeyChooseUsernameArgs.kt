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

package com.nunchuk.android.signer.software.components.primarykey.chooseusername

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs

data class PKeyChooseUsernameArgs(
    val mnemonic: String,
    val passphrase: String,
    val signerName: String
) :
    ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, PKeyChooseUsernameActivity::class.java).apply {
            putExtra(EXTRA_MNEMONIC, mnemonic)
            putExtra(EXTRA_PASSPHRASE, passphrase)
            putExtra(EXTRA_SIGNER_NAME, signerName)
        }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_PASSPHRASE = "EXTRA_PASSPHRASE"
        private const val EXTRA_SIGNER_NAME = "EXTRA_SIGNER_NAME"

        fun deserializeFrom(intent: Intent) = PKeyChooseUsernameArgs(
            intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            intent.extras?.getString(EXTRA_PASSPHRASE, "").orEmpty(),
            intent.extras?.getString(EXTRA_SIGNER_NAME, "").orEmpty(),
        )
    }
}