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
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.nav.args.MiniscriptArgs

interface AppNavigator {
    fun restartApp(activityContext: Context)

    fun openMembershipActivity(
        activityContext: Activity,
        launcher: ActivityResultLauncher<Intent>? = null,
        groupStep: MembershipStage,
        walletId: String? = null,
        groupId: String? = null,
        isPersonalWallet: Boolean = false,
        walletType: GroupWalletType? = null,
        isClearTop: Boolean = false,
        quickWalletParam: QuickWalletParam? = null
    )

    fun openConfigServerKeyActivity(
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: KeyPolicy? = null,
        xfp: String? = null,
        originalKeyPolicy: KeyPolicy? = null,
    )

    fun openConfigGroupServerKeyActivity(
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: GroupKeyPolicy? = null,
        xfp: String? = null,
        groupId: String? = null,
        originalKeyPolicy: GroupKeyPolicy? = null,
    )

    /**
     * @param role for Byzantine
     */
    fun openKeyRecoveryScreen(activityContext: Context, role: String? = null)
    fun openEmergencyLockdownScreen(
        activityContext: Context,
        verifyToken: String,
        groupId: String? = null,
        walletId: String? = null
    )

    /**
     * @param verifyToken for view/update inheritance [InheritancePlanFlow.VIEW]
     * @param inheritance for view/update inheritance [InheritancePlanFlow.VIEW]
     */
    fun openInheritancePlanningScreen(
        launcher: ActivityResultLauncher<Intent>? = null,
        walletId: String = "",
        activityContext: Context,
        verifyToken: String? = null,
        inheritance: Inheritance? = null,
        @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
        @InheritanceSourceFlow.InheritanceSourceFlowInfo sourceFlow: Int = InheritanceSourceFlow.NONE,
        groupId: String? = null,
        dummyTransactionId: String? = null
    )

    fun openWalletAuthentication(
        walletId: String,
        userData: String = "",
        requiredSignatures: Int,
        type: String,
        launcher: ActivityResultLauncher<Intent>? = null,
        activityContext: Activity,
        groupId: String? = null,
        dummyTransactionId: String? = null,
        action: String? = null,
        newEmail: String? = null
    )

    fun openGroupDashboardScreen(
        groupId: String? = null,
        walletId: String? = null,
        message: String? = null,
        activityContext: Context
    )

    fun openPrimaryOwnerScreen(
        activityContext: Context,
        groupId: String,
        walletId: String,
        @PrimaryOwnerFlow.PrimaryOwnerFlowInfo flowInfo: Int
    )

    fun openHotWalletScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Context,
        quickWalletParam: QuickWalletParam? = null,
    )

    fun openOnBoardingScreen(activityContext: Context)

    /**
     * @param selectedTagIds, selectedCollectionIds, feeRate ,antiFeeSniping for [RollOverWalletFlow.PREVIEW]
     */
    fun openRollOverWalletScreen(
        activityContext: Context,
        oldWalletId: String,
        newWalletId: String,
        @RollOverWalletFlow.RollOverWalletFlowInfo startScreen: Int,
        selectedTagIds: List<Int> = emptyList(),
        selectedCollectionIds: List<Int> = emptyList(),
        feeRate: Amount = Amount.ZER0,
        source: Int,
        antiFeeSniping: Boolean = false,
    )

    fun openReferralScreen(activityContext: Context, args: ReferralArgs)
    fun openFreeGroupWalletScreen(
        activityContext: Context,
        walletId: String? = null,
        groupId: String? = null,
        quickWalletParam: QuickWalletParam? = null
    )

    fun openFreeGroupWalletRecoverScreen(
        activityContext: Context,
        walletId: String,
        filePath: String = "",
        qrList: List<String> = emptyList(),
        quickWalletParam: QuickWalletParam?
    )

    fun openMiniscriptScreen(
        activityContext: Context,
        args: MiniscriptArgs
    )
}