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

package com.nunchuk.android.core.qr

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.ExportWalletQRCodeType

data class DynamicQRCodeArgs(val walletId: String, val qrCodeType: Int) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, DynamicQRCodeActivity::class.java).apply {
        putExtra(EXTRA_WALLET_ID, walletId)
        putExtra(EXTRA_QR_CODE_TYPE, qrCodeType)
    }

    companion object {
        const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        const val EXTRA_QR_CODE_TYPE = "EXTRA_QR_CODE_TYPE"

        fun deserializeFrom(intent: Intent): DynamicQRCodeArgs = DynamicQRCodeArgs(
            walletId = intent.extras?.getString(EXTRA_WALLET_ID, "").orEmpty(),
            qrCodeType = intent.extras?.getInt(EXTRA_QR_CODE_TYPE) ?: ExportWalletQRCodeType.BC_UR2_LEGACY,
        )
    }
}