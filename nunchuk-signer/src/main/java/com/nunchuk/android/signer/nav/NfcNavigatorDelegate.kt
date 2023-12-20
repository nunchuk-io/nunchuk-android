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

package com.nunchuk.android.signer.nav

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.nav.NfcNavigator
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.signer.mk4.Mk4Activity
import com.nunchuk.android.signer.tapsigner.NfcSetupActivity

interface NfcNavigatorDelegate : NfcNavigator {
    override fun openSetupMk4(
        activity: Activity,
        fromMembershipFlow: Boolean,
        action: ColdcardAction,
        groupId: String,
        newIndex: Int,
        xfp: String?,
    ) {
        Mk4Activity.navigate(
            activity = activity,
            isMembershipFlow = fromMembershipFlow,
            action = action,
            groupId = groupId,
            newIndex = newIndex,
            xfp = xfp,
        )
    }

    override fun startSetupMk4ForResult(
        launcher: ActivityResultLauncher<Intent>,
        activity: Activity,
        fromMembershipFlow: Boolean,
        action: ColdcardAction,
        groupId: String,
        newIndex: Int,
        xfp: String?,
    ) {
        launcher.launch(
            Mk4Activity.buildIntent(
                activity = activity,
                isMembershipFlow = fromMembershipFlow,
                action = action,
                groupId = groupId,
                newIndex = newIndex,
                xfp = xfp,
            )
        )
    }

    override fun openSetupTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        groupId: String,
    ) {
        activity.startActivity(
            NfcSetupActivity.buildIntent(
                activity = activity,
                setUpAction = NfcSetupActivity.SETUP_TAP_SIGNER,
                fromMembershipFlow = fromMembershipFlow,
                groupId = groupId,
            )
        )
    }

    override fun openVerifyBackupTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        backUpFilePath: String,
        masterSignerId: String,
        groupId: String,
    ) {
        activity.startActivity(
            NfcSetupActivity.buildIntent(
                activity = activity,
                setUpAction = NfcSetupActivity.VERIFY_TAP_SIGNER,
                fromMembershipFlow = fromMembershipFlow,
                backUpFilePath = backUpFilePath,
                masterSignerId = masterSignerId,
                groupId = groupId,
            )
        )
    }

    override fun openCreateBackUpTapSigner(
        activity: Activity,
        fromMembershipFlow: Boolean,
        masterSignerId: String,
        groupId: String,
        signerIndex: Int
    ) {
        activity.startActivity(
            NfcSetupActivity.buildIntent(
                activity = activity,
                setUpAction = NfcSetupActivity.CREATE_BACK_UP_KEY,
                fromMembershipFlow = fromMembershipFlow,
                masterSignerId = masterSignerId,
                groupId = groupId,
                signerIndex = signerIndex,
            )
        )
    }
}