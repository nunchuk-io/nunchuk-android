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

package com.nunchuk.android.signer.software.components.name

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.signer.KeyFlow

data class AddSoftwareSignerNameArgs(
    val mnemonic: String,
    val primaryKeyFlow: Int,
    val username: String?,
    val passphrase: String,
    val address: String?,
    val walletId: String?,
    val groupId: String?,
    val xprv: String?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, AddSoftwareSignerNameActivity::class.java).apply {
            putExtra(EXTRA_MNEMONIC, mnemonic)
            putExtra(EXTRA_PRIMARY_KEY_FLOW, primaryKeyFlow)
            putExtra(EXTRA_PRIMARY_KEY_USERNAME, username)
            putExtra(EXTRA_PRIMARY_KEY_PASSPHRASE, passphrase)
            putExtra(EXTRA_PRIMARY_KEY_ADDRESS, address)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_XPRV, xprv)
            putExtra(EXTRA_GROUP_ID, groupId)
        }

    companion object {
        private const val EXTRA_MNEMONIC = "EXTRA_MNEMONIC"
        private const val EXTRA_PRIMARY_KEY_FLOW = "EXTRA_PRIMARY_KEY_FLOW"
        private const val EXTRA_PRIMARY_KEY_USERNAME = "EXTRA_PRIMARY_KEY_USERNAME"
        private const val EXTRA_PRIMARY_KEY_PASSPHRASE = "EXTRA_PRIMARY_KEY_PASSPHRASE"
        private const val EXTRA_PRIMARY_KEY_ADDRESS = "EXTRA_PRIMARY_KEY_ADDRESS"
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_XPRV = "EXTRA_XPRV"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"

        fun deserializeFrom(intent: Intent) = AddSoftwareSignerNameArgs(
            mnemonic = intent.extras?.getString(EXTRA_MNEMONIC, "").orEmpty(),
            primaryKeyFlow = intent.extras?.getInt(EXTRA_PRIMARY_KEY_FLOW, KeyFlow.NONE)
                ?: KeyFlow.NONE,
            username = intent.extras?.getString(EXTRA_PRIMARY_KEY_USERNAME, "").orEmpty(),
            passphrase = intent.extras?.getString(EXTRA_PRIMARY_KEY_PASSPHRASE, "").orEmpty(),
            address = intent.extras?.getString(EXTRA_PRIMARY_KEY_ADDRESS, "").orEmpty(),
            walletId = intent.extras?.getString(EXTRA_WALLET_ID, "").orEmpty(),
            xprv = intent.extras?.getString(EXTRA_XPRV, "").orEmpty(),
            groupId = intent.extras?.getString(EXTRA_GROUP_ID, "").orEmpty()
        )
    }
}