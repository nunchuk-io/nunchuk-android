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

package com.nunchuk.android.transaction.components.imports

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.share.model.SignFlowType
import com.nunchuk.android.utils.parcelable

data class ImportTransactionArgs(
    val walletId: String,
    val masterFingerPrint: String,
    val initEventId: String,
    val signFlowType: SignFlowType,
    val isFinishWhenError: Boolean
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ImportTransactionActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_MASTER_FINGER_PRINT, masterFingerPrint)
        putExtra(EXTRA_INIT_EVENT_ID, initEventId)
        putExtra(EXTRA_SIGN_FLOW_TYPE, signFlowType)
        putExtra(EXTRA_IS_FINISH_WHEN_ERROR, isFinishWhenError)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "a"
        private const val EXTRA_MASTER_FINGER_PRINT = "c"
        private const val EXTRA_INIT_EVENT_ID = "d"
        private const val EXTRA_SIGN_FLOW_TYPE = "e"
        private const val EXTRA_IS_FINISH_WHEN_ERROR = "f"

        fun deserializeFrom(intent: Intent): ImportTransactionArgs {
            val extras = intent.extras
            return ImportTransactionArgs(
                walletId = extras.getStringValue(EXTRA_WALLET_ID),
                masterFingerPrint = extras.getStringValue(EXTRA_MASTER_FINGER_PRINT),
                initEventId = extras.getStringValue(EXTRA_INIT_EVENT_ID),
                signFlowType = extras?.parcelable<SignFlowType>(EXTRA_SIGN_FLOW_TYPE) ?: SignFlowType.Normal,
                isFinishWhenError = extras?.getBoolean(EXTRA_IS_FINISH_WHEN_ERROR) ?: false
            )
        }
    }
}