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

package com.nunchuk.android.core.nfc

import android.content.Context
import com.nunchuk.android.core.R
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_AUTO_CARD_STATUS
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_EXPORT_WALLET_TO_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_GENERATE_HEAL_CHECK_MSG
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_ADD_KEY
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_EXPORT_TRANSACTION
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_IMPORT_SIGNATURE
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_CHANGE_CVC
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_STATUS

fun shouldShowInputCvcFirst(requestCode: Int) = requestCode != REQUEST_NFC_STATUS
        && requestCode != REQUEST_NFC_CHANGE_CVC
        && requestCode != REQUEST_AUTO_CARD_STATUS
        && requestCode != REQUEST_MK4_ADD_KEY
        && requestCode != REQUEST_EXPORT_WALLET_TO_MK4
        && requestCode != REQUEST_MK4_EXPORT_TRANSACTION
        && requestCode != REQUEST_MK4_IMPORT_SIGNATURE
        && requestCode != REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
        && requestCode != REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
        && requestCode != REQUEST_GENERATE_HEAL_CHECK_MSG

fun isMk4Request(requestCode: Int) = requestCode == REQUEST_MK4_ADD_KEY
        || requestCode == REQUEST_EXPORT_WALLET_TO_MK4
        || requestCode == REQUEST_MK4_EXPORT_TRANSACTION
        || requestCode == REQUEST_MK4_IMPORT_SIGNATURE
        || requestCode == REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
        || requestCode == REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
        || requestCode == REQUEST_GENERATE_HEAL_CHECK_MSG

fun getMk4Hint(context: Context, requestCode: Int) = when (requestCode) {
    REQUEST_MK4_ADD_KEY -> context.getString(R.string.nc_hint_add_mk4)
    REQUEST_EXPORT_WALLET_TO_MK4 -> context.getString(R.string.nc_hint_export_wallet_mk4)
    REQUEST_MK4_EXPORT_TRANSACTION -> context.getString(R.string.nc_hint_export_trans_to_mk4)
    REQUEST_MK4_IMPORT_SIGNATURE -> context.getString(R.string.nc_hint_import_signature_from_mk4)
    REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 -> context.getString(R.string.nc_hint_import_wallet_from_mk4)
    REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 -> context.getString(R.string.nc_hint_import_single_sig_wallet_from_mk4)
    REQUEST_GENERATE_HEAL_CHECK_MSG -> context.getString(R.string.nc_hint_health_check_coldcard)
    else -> ""
}