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

package com.nunchuk.android.signer.components.details

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.compose.dialog.NcConfirmationDialog
import com.nunchuk.android.signer.R
import java.util.Locale

@Composable
fun SecurityTimeoutDialog(
    remainingTimeMs: Long,
    isXprv: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit = onDismiss,
) {
    val totalMinutes = (remainingTimeMs / (60 * 1000)).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val timeString = String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)

    val message = stringResource(
        id = if (isXprv) R.string.nc_security_timeout_message_xprv else R.string.nc_security_timeout_message,
        timeString
    )

    NcConfirmationDialog(
        title = stringResource(id = R.string.nc_security_timeout),
        message = message,
        positiveButtonText = stringResource(id = R.string.nc_confirm),
        negativeButtonText = stringResource(id = com.nunchuk.android.core.R.string.nc_cancel),
        onPositiveClick = onConfirm,
        onDismiss = onDismiss
    )
}

