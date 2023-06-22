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

package com.nunchuk.android.wallet.components.backup

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue

data class BackupWalletArgs(val walletId: String, val numberOfSignKey: Int, val isQuickWallet: Boolean) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, BackupWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_WALLET_TOTAL_SIGN, numberOfSignKey)
        putExtra(EXTRA_IS_QUICK_WALLET, isQuickWallet)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_WALLET_TOTAL_SIGN = "EXTRA_WALLET_TOTAL_SIGN"
        private const val EXTRA_IS_QUICK_WALLET = "EXTRA_IS_QUICK_WALLET"

        fun deserializeFrom(intent: Intent): BackupWalletArgs = BackupWalletArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID),
            numberOfSignKey = intent.getIntExtra(EXTRA_WALLET_TOTAL_SIGN, 0),
            isQuickWallet = intent.getBooleanExtra(EXTRA_IS_QUICK_WALLET, false),
        )
    }
}