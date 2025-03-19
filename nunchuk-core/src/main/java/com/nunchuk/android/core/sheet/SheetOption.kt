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
    @StringRes val subStringId: Int = 0,
    val isDeleted: Boolean = false,
    val label: String? = null,
    val id: String? = null,
    val isSelected: Boolean = false,
    val showDivider: Boolean = false,
    val applyTint: Boolean = true,
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

    const val TYPE_SATSCARD_SKIP_SLOT = 42

    const val TYPE_LOCK_COIN = 43
    const val TYPE_UNLOCK_COIN = 44
    const val TYPE_ADD_COLLECTION = 45
    const val TYPE_ADD_TAG = 46
    const val TYPE_VIEW_TAG = 47
    const val TYPE_VIEW_COLLECTION = 48
    const val TYPE_VIEW_LOCKED_COIN = 49
    const val TYPE_SHOW_OUTPOINT = 50
    const val TYPE_DELETE_TAG = 51
    const val TYPE_REMOVE_COIN_FROM_TAG = 52
    const val TYPE_DELETE_COLLECTION = 53
    const val TYPE_VIEW_COLLECTION_SETTING = 54
    const val TYPE_REMOVE_COIN_FROM_COLLECTION = 55
    const val TYPE_MOVE_COIN_TO_COLLECTION = 56

    const val TYPE_IMPORT_TX_COIN_CONTROL = 57
    const val TYPE_EXPORT_TX_COIN_CONTROL = 58
    const val TYPE_IMPORT_NUNCHUK = 59
    const val TYPE_IMPORT_BIP329 = 60
    const val TYPE_EXPORT_NUNCHUK = 61
    const val TYPE_EXPORT_BIP329 = 62
    const val TYPE_CONFIGURE_GAP_LIMIT = 63

    const val TYPE_ADD_LEDGER = 64
    const val TYPE_ADD_TREZOR = 65
    const val TYPE_ADD_COLDCARD_USB = 66

    const val TYPE_GROUP_WALLET = 67
    const val TYPE_PERSONAL_WALLET = 68
    const val TYPE_PLATFORM_KEY_POLICY = 69
    const val TYPE_EMERGENCY_LOCKDOWN = 70
    const val TYPE_RECURRING_PAYMENT = 71
    const val TYPE_GROUP_CHAT_HISTORY = 72

    const val TYPE_ADD_BITBOX = 73

    const val TYPE_FORCE_SYNC_DUMMY_TX = 74
    const val TYPE_ADDRESS_DERIVATION_PATH = 75
    const val TYPE_VERIFY_ADDRESS_DEVICE = 76

    const val TYPE_EDIT_PRIMARY_OWNER = 77
    const val TYPE_QR_BC_UR2_LEGACY = 78
    const val TYPE_QR_BC_UR2 = 79
    const val TYPE_SEARCH_TX = 80

    const val TYPE_CONSOLIDATE_COIN = 81
    const val TYPE_EXPORT_BBQR = 82

    const val TYPE_ADD_SOFTWARE_KEY = 83
    const val TYPE_ADD_COLDCARD_QR = 84

    const val TYPE_CREATE_NEW_WALLET = 85
    const val TYPE_CREATE_HOT_WALLET = 86

    const val TYPE_REPLACE_KEY = 87

    const val TYPE_CREATE_NEW_DECOY_WALLET = 88

    const val TYPE_ROLL_OVER_ANOTHER_WALLET = 89

    const val TYPE_EXPORT_PORTAL = 90
    const val TYPE_EXPORT_TX_INVOICES = 91
    const val TYPE_MARK_ADDRESS_AS_USED = 92

    const val TYPE_ADD_INHERITANCE_NFC = 93
    const val TYPE_ADD_INHERITANCE_COLDCARD = 94
    const val TYPE_SAVE_FILE = 95
    const val TYPE_SHARE_FILE = 96
    const val TYPE_EXPORT_TX_AS_PDF = 97
    const val TYPE_EXPORT_TX_AS_CSV = 98
}