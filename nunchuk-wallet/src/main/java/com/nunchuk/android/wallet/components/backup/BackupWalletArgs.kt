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
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.utils.parcelable

data class BackupWalletArgs(
    val wallet: Wallet,
    val quickWalletParam: QuickWalletParam?,
    val isDecoyWallet: Boolean
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent(activityContext, BackupWalletActivity::class.java).apply {
            putExtra(EXTRA_WALLET, wallet)
            putExtra(EXTRA_IS_DECOY_WALLET, isDecoyWallet)
            putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
        }

    companion object {
        private const val EXTRA_WALLET = "EXTRA_WALLET"
        private const val EXTRA_IS_DECOY_WALLET = "EXTRA_IS_DECOY_WALLET"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun deserializeFrom(intent: Intent): BackupWalletArgs = BackupWalletArgs(
            wallet = intent.parcelable<Wallet>(EXTRA_WALLET)!!,
            isDecoyWallet = intent.getBooleanExtra(EXTRA_IS_DECOY_WALLET, false),
            quickWalletParam = intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM),
        )
    }
}