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
import com.nunchuk.android.app.miniscript.MiniscriptActivity
import com.nunchuk.android.app.onboard.OnboardActivity
import com.nunchuk.android.app.referral.ReferralActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.wallet.QuickWalletActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.referral.ReferralArgs
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.main.MainComposeActivity
import com.nunchuk.android.main.components.tabs.services.emergencylockdown.EmergencyLockdownActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.ClaimInheritanceActivity
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoveryActivity
import com.nunchuk.android.main.groupwallet.FreeGroupWalletActivity
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivity
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardActivity
import com.nunchuk.android.main.membership.byzantine.primaryowner.PrimaryOwnerActivity
import com.nunchuk.android.main.membership.key.desktop.AddDesktopKeyActivity
import com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase.BackUpSeedPhraseActivity
import com.nunchuk.android.main.membership.onchaintimelock.checkfirmware.CheckFirmwareActivity
import com.nunchuk.android.main.membership.policy.ConfigServerKeyActivity
import com.nunchuk.android.main.rollover.RollOverWalletActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.GroupKeyPolicy
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.nav.args.CheckFirmwareArgs
import com.nunchuk.android.nav.args.ClaimArgs
import com.nunchuk.android.nav.args.MainComposeArgs
import com.nunchuk.android.nav.args.MembershipArgs
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.NfcNavigatorDelegate
import com.nunchuk.android.signer.nav.SignerNavigatorDelegate
import com.nunchuk.android.transaction.nav.TransactionNavigatorDelegate
import com.nunchuk.android.type.SignerTag
import com.nunchuk.android.type.WalletType
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
        bottomNavViewPosition: Int?,
        messages: ArrayList<String>?,
        isClearTask: Boolean,
        askPin: Boolean,
    ) {
        MainActivity.start(
            activityContext = activityContext,
            position = bottomNavViewPosition,
            messages = messages,
            isClearTask = isClearTask,
            askPin = askPin,
        )
    }

    override fun openMainComposeScreen(activity: Activity, args: MainComposeArgs) {
        MainComposeActivity.start(activity, args)
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

    override fun openQuickWalletScreen(
        activityContext: Context,
        quickWalletParam: QuickWalletParam?
    ) {
        QuickWalletActivity.navigate(activityContext, quickWalletParam)
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

    override fun openReferralScreen(activityContext: Context, args: ReferralArgs) {
        ReferralActivity.buildIntent(activityContext, args).let {
            activityContext.startActivity(it)
        }
    }

    override fun openFreeGroupWalletScreen(
        activityContext: Context,
        walletId: String?,
        groupId: String?,
        quickWalletParam: QuickWalletParam?
    ) {
        FreeGroupWalletActivity.start(
            context = activityContext,
            groupId = groupId,
            walletId = walletId
        )
    }

    override fun openFreeGroupWalletRecoverScreen(
        activityContext: Context,
        walletId: String,
        filePath: String,
        qrList: List<String>,
        quickWalletParam: QuickWalletParam?
    ) {
        FreeGroupWalletActivity.startRecover(
            activityContext,
            walletId,
            filePath,
            qrList = qrList,
            quickWalletParam = quickWalletParam
        )
    }

    override fun openAddDesktopKey(
        activity: Activity,
        signerTag: SignerTag,
        groupId: String?,
        step: MembershipStep,
        isInheritanceKey: Boolean
    ) {
        AddDesktopKeyActivity.navigate(
            activity = activity,
            signerTag = signerTag,
            groupId = groupId,
            step = step,
            isAddInheritanceKey = isInheritanceKey
        )
    }
}

interface AppNavigatorDelegate : AppNavigator {

    override fun restartApp(activityContext: Context) {
        SplashActivity.navigate(activityContext)
    }

    override fun openMembershipActivity(
        activityContext: Activity,
        launcher: ActivityResultLauncher<Intent>?,
        groupStep: MembershipStage,
        walletId: String?,
        groupId: String?,
        isPersonalWallet: Boolean,
        groupWalletType: GroupWalletType?,
        slug: String?,
        walletTypeName: String?,
        walletType: WalletType?,
        isClearTop: Boolean,
        quickWalletParam: QuickWalletParam?,
        inheritanceType: String?,
        replacedWalletId: String?,
        changeTimelockFlow: Int
    ) {
        val args = MembershipArgs(
            groupStep = groupStep,
            walletId = walletId,
            groupId = groupId,
            isPersonalWallet = isPersonalWallet,
            groupWalletType = groupWalletType,
            slug = slug,
            walletTypeName = walletTypeName,
            walletType = walletType,
            quickWalletParam = quickWalletParam,
            inheritanceType = inheritanceType,
            replacedWalletId = replacedWalletId,
            changeTimelockFlow = changeTimelockFlow
        )

        val intent = if (groupStep == MembershipStage.CREATE_WALLET_SUCCESS) {
            MembershipActivity.openWalletCreatedSuccessIntent(
                activity = activityContext,
                args = args
            )
        } else {
            MembershipActivity.buildIntent(
                activity = activityContext,
                args = args
            )
        }.apply {
            if (isClearTop) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        if (launcher != null) {
            launcher.launch(intent)
        } else {
            activityContext.startActivity(intent)
        }
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

    override fun openClaimInheritanceScreen(
        activityContext: Context,
        args: ClaimArgs
    ) {
        val intent = Intent(activityContext, ClaimInheritanceActivity::class.java)
        intent.putExtras(args.buildBundle())
        activityContext.startActivity(intent)
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

    override fun openHotWalletScreen(
        launcher: ActivityResultLauncher<Intent>?,
        activityContext: Context,
        quickWalletParam: QuickWalletParam?
    ) {
        OnboardActivity.openHotWalletIntroScreen(
            launcher,
            activityContext,
            quickWalletParam = quickWalletParam
        )
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
        source: Int,
        antiFeeSniping: Boolean,
    ) {
        RollOverWalletActivity.navigate(
            activity = activityContext,
            oldWalletId = oldWalletId,
            newWalletId = newWalletId,
            startScreen = startScreen,
            selectedTagIds = selectedTagIds,
            selectedCollectionIds = selectedCollectionIds,
            feeRate = feeRate,
            source = source,
            antiFeeSniping = antiFeeSniping
        )
    }

    override fun openMiniscriptScreen(
        activityContext: Context,
        args: MiniscriptArgs
    ) {
        MiniscriptActivity.start(activityContext, args)
    }

    override fun openCheckFirmwareActivity(
        activityContext: Context,
        launcher: ActivityResultLauncher<Intent>?,
        args: CheckFirmwareArgs
    ) {
        CheckFirmwareActivity.navigate(activityContext, launcher, args)
    }

    override fun openBackUpSeedPhraseActivity(
        activityContext: Context,
        args: BackUpSeedPhraseArgs
    ) {
        BackUpSeedPhraseActivity.start(activityContext, args)
    }

    override fun returnMembershipScreen() {
        ActivityManager.popUntil(MembershipActivity::class.java)
    }

    override fun returnToClaimScreen(activityContext: Context) {
        returnToPreviousScreen(
            activityClass = ClaimInheritanceActivity::class.java,
            activityContext = activityContext,
        )
    }
}