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

package com.nunchuk.android.signer.satscard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.SatsCardStatus
import com.nunchuk.android.utils.parcelable

data class SatsCardArgs (val status: SatsCardStatus, val hasWallet: Boolean, val isShowUnseal: Boolean) : ActivityArgs {
    override fun buildIntent(activityContext: Context): Intent {
        return Intent(activityContext, SatsCardActivity::class.java).apply {
            putExtra(EXTRA_SATSCARD_STATUS, status)
            putExtra(EXTRA_HAS_WALLET, hasWallet)
            putExtra(EXTRA_SHOW_UNSEAL_SLOT, isShowUnseal)
        }
    }

    companion object {
        const val EXTRA_SATSCARD_STATUS = "extra_satscard_status"
        private const val EXTRA_HAS_WALLET = "hasWallet" // don't change it, refer to satsCardUnsealSlotFragment in navigation
        private const val EXTRA_SHOW_UNSEAL_SLOT = "EXTRA_SHOW_UNSEAL_SLOT"

        fun deserializeBundle(arguments: Bundle) = SatsCardArgs(
            arguments.parcelable(EXTRA_SATSCARD_STATUS)!!,
            arguments.getBoolean(EXTRA_HAS_WALLET),
            arguments.getBoolean(EXTRA_SHOW_UNSEAL_SLOT),
        )
    }
}