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

package com.nunchuk.android.core.sheet

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize


@Parcelize
data class SheetOption(
    val type: Int = 0,
    @DrawableRes val resId: Int = 0,
    @StringRes val stringId: Int = 0,
    val isDeleted: Boolean = false,
    val label: String? = null,
    val id: String? = null,
    val isSelected: Boolean = false
) : Parcelable

object SheetOptionType {
    // wallet detail
    const val TYPE_IMPORT_PSBT = 1
    const val TYPE_IMPORT_PSBT_QR = 2
    const val TYPE_SAVE_WALLET_CONFIG = 3

    // wallet config
    const val TYPE_EXPORT_AS_QR = 6
    const val TYPE_EXPORT_TO_COLD_CARD = 9
    const val TYPE_DELETE_WALLET = 10

    // Sats card
    const val TYPE_VIEW_SATSCARD_UNSEAL = 11

    // Sweep option
    const val TYPE_SWEEP_TO_WALLET = 12
    const val TYPE_SWEEP_TO_EXTERNAL_ADDRESS = 13

    // Mk4
    const val EXPORT_COLDCARD_VIA_NFC = 14
    const val EXPORT_COLDCARD_VIA_FILE = 15
    const val EXPORT_TX_TO_Mk4 = 16
    const val IMPORT_TX_FROM_Mk4 = 17
    const val IMPORT_SINGLE_SIG_COLD_CARD = 18
    const val IMPORT_MULTI_SIG_COLD_CARD = 19

    // Membership
    const val TYPE_RESTART_WIZARD = 20
    const val TYPE_EXIT_WIZARD = 21
    const val TYPE_ADD_COLDCARD_NFC = 22
    const val TYPE_ADD_COLDCARD_FILE = 23
    const val TYPE_CANCEL = 24
    const val TYPE_ONE_OPTION_CONFIRM = 25
    const val TYPE_FORCE_REFRESH_WALLET = 26

    // Chat Action
    const val CHAT_ACTION_TAKE_PHOTO = 26
    const val CHAT_ACTION_CAPTURE_VIDEO = 27
    const val CHAT_ACTION_SELECT_PHOTO_VIDEO = 28
    const val CHAT_ACTION_SELECT_FILE = 29

    const val SET_UP_INHERITANCE = 30

    const val TYPE_ADD_AIRGAP_KEYSTONE = 31
    const val TYPE_ADD_AIRGAP_JADE = 32
    const val TYPE_ADD_AIRGAP_PASSPORT = 33
    const val TYPE_ADD_AIRGAP_SEEDSIGNER = 34
    const val TYPE_ADD_AIRGAP_OTHER = 35

    const val TYPE_EXPORT_QR = 36
    const val TYPE_EXPORT_FILE = 37
    const val TYPE_IMPORT_QR = 38
    const val TYPE_IMPORT_FILE = 39

    const val TYPE_IMPORT_TX = 40
    const val TYPE_EXPORT_BSMS = 41
}