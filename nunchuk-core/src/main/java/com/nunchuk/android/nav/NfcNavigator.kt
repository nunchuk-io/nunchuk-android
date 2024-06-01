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

package com.nunchuk.android.nav

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.share.ColdcardAction

interface NfcNavigator {
    fun openSetupMk4(
        activity: Activity,
        fromMembershipFlow: Boolean,
        action: ColdcardAction = ColdcardAction.CREATE,
        groupId: String = "",
        newIndex: Int = -1,
        xfp: String? = null,
        isScanQRCode: Boolean = false,
        replacedXfp: String? = null,
        walletId: String? = null,
    )

    fun startSetupMk4ForResult(
        launcher: ActivityResultLauncher<Intent>,
        activity: Activity,
        fromMembershipFlow: Boolean,
        action: ColdcardAction = ColdcardAction.CREATE,
        groupId: String = "",
        newIndex: Int = -1,
        xfp: String? = null,
        replacedXfp: String? = null,
        walletId: String? = null,
    )

    fun openSetupTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        groupId: String = "",
        replacedXfp: String = "",
        walletId: String = "",
    )

    fun openVerifyBackupTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        backUpFilePath: String,
        masterSignerId: String,
        groupId: String = "",
        keyId: String = "",
    )

    fun openCreateBackUpTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        masterSignerId: String,
        groupId: String = "",
        signerIndex: Int = 0,
        replacedXfp: String = "",
        walletId: String = "",
    )
}