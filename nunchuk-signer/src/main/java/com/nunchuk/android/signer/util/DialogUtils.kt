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

package com.nunchuk.android.signer.util

import android.app.Activity
import com.nunchuk.android.model.TapSignerStatus
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog

fun Activity.handleTapSignerStatus(status: TapSignerStatus?, onCreateSigner: () -> Unit, onSetupNfc: () -> Unit) {
    status ?: return
    if (status.isNeedSetup.not()) {
        if (status.isCreateSigner) {
            showNfcAlreadyAdded()
        } else {
            onCreateSigner()
        }
    } else {
        showSetupNfc(onSetupNfc)
    }
}

internal fun Activity.showNfcAlreadyAdded() {
    NCInfoDialog(this).showDialog(
        message = getString(R.string.nc_nfc_key_already_added),
    ).show()
}

internal fun Activity.showSetupNfc(callback : () -> Unit) {
    NCWarningDialog(this).showDialog(
        title = getString(R.string.nc_set_up_nfc_key),
        message = getString(R.string.nc_setup_nfc_key_desc),
        onYesClick = callback
    ).show()
}