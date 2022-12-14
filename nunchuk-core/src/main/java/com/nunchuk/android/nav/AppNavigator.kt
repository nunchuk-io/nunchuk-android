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

package com.nunchuk.android.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage

interface AppNavigator {
    fun restartApp(activityContext: Context)

    fun openMembershipActivity(
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: KeyPolicy? = null,
        xfp: String? = null
    )

    fun openMembershipActivity(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: KeyPolicy? = null,
        xfp: String? = null
    )

    fun openKeyRecoveryScreen(activityContext: Context)
    fun openEmergencyLockdownScreen(activityContext: Context, verifyToken: String)

    /**
     * @param verifyToken for view/update inheritance [InheritancePlanFlow.VIEW]
     * @param inheritance for view/update inheritance [InheritancePlanFlow.VIEW]
     */
    fun openInheritancePlanningScreen(
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Context,
        verifyToken: String? = null,
        inheritance: Inheritance? = null,
        @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int
    )

    fun openWalletAuthentication(
        walletId: String,
        userData: String,
        requiredSignatures: Int,
        type: String,
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity
    )
}