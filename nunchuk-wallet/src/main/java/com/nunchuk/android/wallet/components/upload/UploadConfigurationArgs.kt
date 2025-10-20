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

package com.nunchuk.android.wallet.components.upload

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.utils.parcelable

data class UploadConfigurationArgs(
    val walletId: String,
    val isOnChainFlow: Boolean = false,
    val groupId: String? = null,
    val replacedWalletId: String? = null,
    val quickWalletParam: QuickWalletParam? = null
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, UploadConfigurationActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_IS_ON_CHAIN_FLOW, isOnChainFlow)
        putExtra(EXTRA_GROUP_ID, groupId)
        putExtra(EXTRA_REPLACED_WALLET_ID, replacedWalletId)
        putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_IS_ON_CHAIN_FLOW = "EXTRA_IS_ON_CHAIN_FLOW"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_REPLACED_WALLET_ID = "EXTRA_REPLACED_WALLET_ID"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"

        fun deserializeFrom(intent: Intent): UploadConfigurationArgs = UploadConfigurationArgs(
            walletId = intent.extras.getStringValue(EXTRA_WALLET_ID),
            isOnChainFlow = intent.getBooleanExtra(EXTRA_IS_ON_CHAIN_FLOW, false),
            groupId = intent.getStringExtra(EXTRA_GROUP_ID),
            replacedWalletId = intent.getStringExtra(EXTRA_REPLACED_WALLET_ID),
            quickWalletParam = intent.parcelable(EXTRA_QUICK_WALLET_PARAM)
        )
    }
}