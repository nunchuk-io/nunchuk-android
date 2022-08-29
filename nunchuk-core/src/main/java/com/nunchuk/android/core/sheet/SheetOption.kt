package com.nunchuk.android.core.sheet

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SheetOption(val type: Int, @DrawableRes val resId: Int = 0, @StringRes val stringId: Int, val isDeleted: Boolean = false) : Parcelable

object SheetOptionType {
    // wallet detail
    const val TYPE_IMPORT_PSBT = 1
    const val TYPE_IMPORT_PSBT_QR = 2
    const val TYPE_SAVE_WALLET_CONFIG = 3
    const val TYPE_PSBT_QR_KEY_STONE = 4
    const val TYPE_PSBT_QR_PASSPORT = 5
    // wallet config
    const val TYPE_EXPORT_AS_QR = 6
    const val TYPE_EXPORT_KEYSTONE_QR = 7
    const val TYPE_EXPORT_PASSPORT_QR = 8
    const val TYPE_EXPORT_TO_COLD_CARD = 9
    const val TYPE_DELETE_WALLET = 10
    // Sats card
    const val TYPE_VIEW_SATSCARD_UNSEAL = 11
    // Sweep option
    const val TYPE_SWEEP_TO_WALLET = 12
    const val TYPE_SWEEP_TO_EXTERNAL_ADDRESS = 13
}