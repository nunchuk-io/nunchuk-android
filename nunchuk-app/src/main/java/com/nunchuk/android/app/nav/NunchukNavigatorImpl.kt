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

package com.nunchuk.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.QuickWalletNavigationDirections
import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.onboard.OnboardActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.wallet.QuickWalletActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.main.components.tabs.services.emergencylockdown.EmergencyLockdownActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoveryActivity
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivity
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.main.membership.byzantine.primaryowner.PrimaryOwnerActivity
import com.nunchuk.android.main.membership.policy.ConfigServerKeyActivity
import com.nunchuk.android.main.rollover.RollOverWalletActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.NfcNavigatorDelegate
import com.nunchuk.android.signer.nav.SignerNavigatorDelegate
import com.nunchuk.android.transaction.nav.TransactionNavigatorDelegate
import com.nunchuk.android.wallet.components.coin.CoinActivity
import com.nunchuk.android.wallet.nav.WalletNavigatorDelegate
import javax.inject.Inject

internal class NunchukNavigatorImpl @Inject constructor() : NunchukNavigator,
    AppNavigatorDelegate,
    AuthNavigatorDelegate,
    SignerNavigatorDelegate,
    WalletNavigatorDelegate,
    TransactionNavigatorDelegate,
    ContactNavigatorDelegate,
    MessageNavigatorDelegate,
    SettingNavigatorDelegate,
    NfcNavigatorDelegate {

    override fun openMainScreen(
        activityContext: Context,
        loginHalfToken: String?,
        deviceId: String?,
        bottomNavViewPosition: Int?,
        messages: ArrayList<String>?,
        isClearTask: Boolean,
    ) {
        MainActivity.start(
            activityContext, loginHalfToken, deviceId, bottomNavViewPosition,
            messages = messages, isClearTask = isClearTask
        )
    }

    override fun returnToMainScreen(activity: Activity) {
        activity.startActivity(
            Intent(activity, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            },
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            activity.overridePendingTransition(0, 0)
        }
    }

    override fun openGuestModeIntroScreen(activityContext: Context) {
        GuestModeIntroActivity.start(activityContext)
    }

    override fun openGuestModeMessageIntroScreen(activityContext: Context) {
        GuestModeMessageIntroActivity.start(activityContext)
    }

    override fun openQuickWalletScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context,
    ) {
        QuickWalletActivity.start(launcher, activityContext)
    }

    override fun openCreateNewSeedScreen(fragment: Fragment, isQuickWallet: Boolean) {
        fragment.findNavController()
            .navigate(QuickWalletNavigationDirections.showCreateNewSeedFragment(isQuickWallet))
    }

    override fun openCoinList(
        launcher: ActivityResultLauncher<Intent>?,
        context: Context,
        walletId: String,
        txId: String,
        inputs: List<UnspentOutput>,
        amount: Double,
    ) {
        val intent = CoinActivity.buildIntent(
            context = context,
            walletId = walletId,
            txId = txId,
            inputs = inputs,
            amount = amount
        )
        launcher?.launch(intent) ?: context.startActivity(intent)
    }

    override fun openCoinDetail(
        launcher: ActivityResultLauncher<Intent>?,
        context: Context,
        walletId: String,
        output: UnspentOutput,
    ) {
        val intent = CoinActivity.buildIntent(
            context = context,
            walletId = walletId,
            output = output,
        )
        launcher?.launch(intent) ?: context.startActivity(intent)
    }

    override fun openPrimaryOwnerScreen(
        activityContext: Context,
        groupId: String,
        walletId: String,
        @PrimaryOwnerFlow.PrimaryOwnerFlowInfo flowInfo: Int,
    ) {
        PrimaryOwnerActivity.navigate(
            activity = activityContext,
            groupId = groupId,
            walletId = walletId,
            flowInfo = flowInfo
        )
    }
}

interface AppNavigatorDelegate : AppNavigator {

    override fun restartApp(activityContext: Context) {
        val intent = Intent(activityContext, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        activityContext.startActivity(intent)
    }

    override fun openMembershipActivity(
        activityContext: Activity,
        groupStep: MembershipStage,
        walletId: String?,
        groupId: String?,
        isPersonalWallet: Boolean,
        walletType: GroupWalletType?,
    ) {
        val intent = MembershipActivity.buildIntent(
            activity = activityContext,
            groupStep = groupStep,
            walletId = walletId,
            groupId = groupId,
            isPersonalWallet = isPersonalWallet,
            walletType = walletType
        )
        activityContext.startActivity(intent)
    }

    override fun openMembershipActivity(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        groupStep: MembershipStage,
        walletId: String?,
        groupId: String?,
        isPersonalWallet: Boolean,
        walletType: GroupWalletType?,
    ) {
        val intent = MembershipActivity.buildIntent(
            activity = activityContext,
            groupStep = groupStep,
            walletId = walletId,
            groupId = groupId,
            isPersonalWallet = isPersonalWallet,
            walletType = walletType
        )
        launcher.launch(intent)
    }

    override fun openConfigServerKeyActivity(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: KeyPolicy?,
        xfp: String?,
        originalKeyPolicy: KeyPolicy?,
    ) {
        launcher?.launch(
            ConfigServerKeyActivity.buildIntent(
                activity = activityContext,
                keyPolicy = keyPolicy,
                xfp = xfp,
                groupStep = groupStep,
                originKeyPolicy = originalKeyPolicy

            )
        ) ?: activityContext.startActivity(
            ConfigServerKeyActivity.buildIntent(
                activity = activityContext,
                keyPolicy = keyPolicy,
                xfp = xfp,
                groupStep = groupStep,
                originKeyPolicy = originalKeyPolicy
            )
        )
    }

    override fun openConfigGroupServerKeyActivity(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: GroupKeyPolicy?,
        xfp: String?,
        groupId: String?,
        originalKeyPolicy: GroupKeyPolicy?
    ) {
        launcher?.launch(
            ConfigServerKeyActivity.buildGroupIntent(
                activity = activityContext,
                keyPolicy = keyPolicy,
                groupId = groupId,
                xfp = xfp,
                groupStep = groupStep,
                originKeyPolicy = originalKeyPolicy
            )
        ) ?: activityContext.startActivity(
            ConfigServerKeyActivity.buildGroupIntent(
                activity = activityContext,
                keyPolicy = keyPolicy,
                groupId = groupId,
                xfp = xfp,
                groupStep = groupStep,
                originKeyPolicy = originalKeyPolicy
            )
        )
    }

    override fun openKeyRecoveryScreen(activityContext: Context, role: String?) {
        KeyRecoveryActivity.navigate(activityContext, role.orEmpty())
    }

    override fun openEmergencyLockdownScreen(
        activityContext: Context,
        verifyToken: String,
        groupId: String?,
        walletId: String?,
    ) {
        EmergencyLockdownActivity.navigate(activityContext, verifyToken, groupId, walletId)
    }

    override fun openInheritancePlanningScreen(
        launcher: ActivityResultLauncher<Intent>?,
        walletId: String,
        activityContext: Context,
        verifyToken: String?,
        inheritance: Inheritance?,
        @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
        @InheritanceSourceFlow.InheritanceSourceFlowInfo sourceFlow: Int,
        groupId: String?,
        dummyTransactionId: String?,
    ) {
        InheritancePlanningActivity.navigate(
            launcher = launcher,
            activity = activityContext,
            verifyToken = verifyToken,
            flowInfo = flowInfo,
            inheritance = inheritance,
            sourceFlow = sourceFlow,
            walletId = walletId,
            groupId = groupId,
            dummyTransactionId = dummyTransactionId
        )
    }

    override fun openWalletAuthentication(
        walletId: String,
        userData: String,
        requiredSignatures: Int,
        type: String,
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Activity,
        groupId: String?,
        dummyTransactionId: String?,
        action: String?,
        newEmail: String?,
    ) {
        WalletAuthenticationActivity.start(
            walletId = walletId,
            userData = userData,
            requiredSignatures = requiredSignatures,
            type = type,
            launcher = launcher,
            activityContext = activityContext,
            groupId = groupId,
            dummyTransactionId = dummyTransactionId,
            action = action,
            newEmail = newEmail
        )
    }

    override fun openGroupDashboardScreen(
        groupId: String?,
        walletId: String?,
        message: String?,
        activityContext: Context,
    ) {
        GroupDashboardActivity.navigate(
            activityContext,
            groupId = groupId,
            walletId = walletId,
            message = message
        )
    }

    override fun openHotWalletScreen(activityContext: Context) {
        OnboardActivity.openHotWalletIntroScreen(activityContext)
    }

    override fun openOnBoardingScreen(activityContext: Context) {
        activityContext.startActivity(Intent(activityContext, OnboardActivity::class.java))
    }

    override fun openRollOverWalletScreen(
        activityContext: Context,
        oldWalletId: String,
        newWalletId: String,
        @RollOverWalletFlow.RollOverWalletFlowInfo startScreen: Int,
        selectedTagIds: List<Int>,
        selectedCollectionIds: List<Int>,
        feeRate: Amount,
    ) {
        RollOverWalletActivity.navigate(activity = activityContext,
            oldWalletId = oldWalletId,
            newWalletId = newWalletId,
            startScreen = startScreen,
            selectedTagIds = selectedTagIds,
            selectedCollectionIds = selectedCollectionIds,
            feeRate = feeRate
        )
    }
}