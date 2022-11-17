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

package com.nunchuk.android.messages.components.detail

import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.isCanceled
import com.nunchuk.android.core.util.isCreated
import com.nunchuk.android.core.util.isPendingKeys
import com.nunchuk.android.core.util.isReadyFinalize
import com.nunchuk.android.model.RoomWallet

fun TextView.bindWalletStatus(roomWallet: RoomWallet) {
    when {
        roomWallet.isCreated() -> bindCreatedStatus()
        roomWallet.isCanceled() -> bindCanceledStatus()
        roomWallet.isPendingKeys() -> bindPendingKeysStatus()
        roomWallet.isReadyFinalize() -> bindReadyFinalizeStatus()
        else -> bindCreatedStatus()
    }
}

private fun TextView.bindCreatedStatus() {
    text = context.getString(R.string.nc_text_completed)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_green_color)
}

private fun TextView.bindReadyFinalizeStatus() {
    text = context.getString(R.string.nc_message_pending_finalization)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_beeswax_tint)
}

private fun TextView.bindPendingKeysStatus() {
    text = context.getString(R.string.nc_message_pending_signers)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_fill_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_red_tint_color)
}

fun TextView.bindCanceledStatus() {
    text = context.getString(R.string.nc_text_canceled)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
}

fun TextView.bindPendingSignatures() {
    text = context.getString(R.string.nc_transaction_pending_signatures)
    background = AppCompatResources.getDrawable(context, R.drawable.nc_rounded_tag_stroke_background)
    backgroundTintList = ContextCompat.getColorStateList(context, R.color.nc_red_tint_color)
}
