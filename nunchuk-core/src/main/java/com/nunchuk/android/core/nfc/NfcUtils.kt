package com.nunchuk.android.core.nfc

import android.content.Context
import com.nunchuk.android.core.R
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_AUTO_CARD_STATUS
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_EXPORT_WALLET_TO_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_ADD_KEY
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_EXPORT_TRANSACTION
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_MK4_IMPORT_TRANSACTION
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_CHANGE_CVC
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_STATUS

fun shouldShowInputCvcFirst(requestCode: Int) = requestCode != REQUEST_NFC_STATUS
        && requestCode != REQUEST_NFC_CHANGE_CVC
        && requestCode != REQUEST_AUTO_CARD_STATUS
        && requestCode != REQUEST_MK4_ADD_KEY
        && requestCode != REQUEST_EXPORT_WALLET_TO_MK4
        && requestCode != REQUEST_MK4_EXPORT_TRANSACTION
        && requestCode != REQUEST_MK4_IMPORT_TRANSACTION
        && requestCode != REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
        && requestCode != REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4

fun isMk4Request(requestCode: Int) = requestCode == REQUEST_MK4_ADD_KEY
        || requestCode == REQUEST_EXPORT_WALLET_TO_MK4
        || requestCode == REQUEST_MK4_EXPORT_TRANSACTION
        || requestCode == REQUEST_MK4_IMPORT_TRANSACTION
        || requestCode == REQUEST_IMPORT_MULTI_WALLET_FROM_MK4
        || requestCode == REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4

fun getMk4Hint(context: Context, requestCode: Int) = when (requestCode) {
    REQUEST_MK4_ADD_KEY -> context.getString(R.string.nc_hint_add_mk4)
    REQUEST_EXPORT_WALLET_TO_MK4 -> context.getString(R.string.nc_hint_export_wallet_mk4)
    REQUEST_MK4_EXPORT_TRANSACTION -> context.getString(R.string.nc_hint_export_trans_to_mk4)
    REQUEST_MK4_IMPORT_TRANSACTION -> context.getString(R.string.nc_hint_import_trans_from_mk4)
    REQUEST_IMPORT_MULTI_WALLET_FROM_MK4 -> context.getString(R.string.nc_hint_import_wallet_from_mk4)
    REQUEST_IMPORT_SINGLE_WALLET_FROM_MK4 -> context.getString(R.string.nc_hint_import_single_sig_wallet_from_mk4)
    else -> ""
}