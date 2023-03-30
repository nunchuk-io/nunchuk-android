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

package com.nunchuk.android.app.nav

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.QuickWalletNavigationDirections
import com.nunchuk.android.app.intro.GuestModeIntroActivity
import com.nunchuk.android.app.intro.GuestModeMessageIntroActivity
import com.nunchuk.android.app.splash.SplashActivity
import com.nunchuk.android.app.wallet.QuickWalletActivity
import com.nunchuk.android.auth.nav.AuthNavigatorDelegate
import com.nunchuk.android.contact.nav.ContactNavigatorDelegate
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.MainActivity
import com.nunchuk.android.main.components.tabs.services.emergencylockdown.EmergencyLockdownActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.keyrecovery.KeyRecoveryActivity
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.authentication.WalletAuthenticationActivity
import com.nunchuk.android.messages.nav.MessageNavigatorDelegate
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.AppNavigator
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.nav.SettingNavigatorDelegate
import com.nunchuk.android.signer.nav.NfcNavigatorDelegate
import com.nunchuk.android.signer.nav.SignerNavigatorDelegate
import com.nunchuk.android.transaction.nav.TransactionNavigatorDelegate
import com.nunchuk.android.wallet.components.coin.CoinActivity
import com.nunchuk.android.wallet.nav.WalletNavigatorDelegate
import javax.inject.Inject

internal class NunchukNavigatorImpl @Inject constructor(
) : NunchukNavigator,
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
        isClearTask: Boolean
    ) {
        MainActivity.start(
            activityContext, loginHalfToken, deviceId, bottomNavViewPosition,
            messages = messages, isClearTask = isClearTask
        )
    }

    override fun returnToMainScreen() {
        ActivityManager.popUntil(MainActivity::class.java)
    }

    override fun openGuestModeIntroScreen(activityContext: Context) {
        GuestModeIntroActivity.start(activityContext)
    }

    override fun openGuestModeMessageIntroScreen(activityContext: Context) {
        GuestModeMessageIntroActivity.start(activityContext)
    }

    override fun openQuickWalletScreen(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Context
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
        amount: Double
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
        txId: String,
        vout: Int
    ) {
        val intent = CoinActivity.buildIntent(
            context = context,
            walletId = walletId,
            txId = txId,
            vout = vout
        )
        launcher?.launch(intent) ?: context.startActivity(intent)
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
        isClearTop: Boolean
    ) {
        val intent = MembershipActivity.buildIntent(
            activity = activityContext,
            groupStep = groupStep,
            walletId = walletId
        )
        if (isClearTop) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        activityContext.startActivity(intent)
    }

    override fun openMembershipActivity(
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity,
        groupStep: MembershipStage,
        keyPolicy: KeyPolicy?,
        xfp: String?
    ) {
        launcher.launch(
            MembershipActivity.buildIntent(
                activity = activityContext,
                groupStep = groupStep,
                keyPolicy = keyPolicy,
                xfp = xfp
            )
        )
    }

    override fun openKeyRecoveryScreen(activityContext: Context) {
        KeyRecoveryActivity.navigate(activityContext)
    }

    override fun openEmergencyLockdownScreen(activityContext: Context, verifyToken: String) {
        EmergencyLockdownActivity.navigate(activityContext, verifyToken)
    }

    override fun openInheritancePlanningScreen(
        walletId: String,
        activityContext: Context,
        verifyToken: String?,
        inheritance: Inheritance?,
        @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
        isOpenFromWizard: Boolean
    ) {
        InheritancePlanningActivity.navigate(
            activity = activityContext,
            verifyToken = verifyToken,
            flowInfo = flowInfo,
            inheritance = inheritance,
            isOpenFromWizard = isOpenFromWizard,
            walletId = walletId,
        )
    }

    override fun openWalletAuthentication(
        walletId: String,
        userData: String,
        requiredSignatures: Int,
        type: String,
        launcher: ActivityResultLauncher<Intent>,
        activityContext: Activity
    ) {
        WalletAuthenticationActivity.start(
            walletId = walletId,
            userData = userData,
            requiredSignatures = requiredSignatures,
            type = type,
            launcher = launcher,
            activityContext = activityContext
        )
    }
}