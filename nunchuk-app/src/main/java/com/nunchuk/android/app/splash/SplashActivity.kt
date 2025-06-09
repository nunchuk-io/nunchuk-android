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

package com.nunchuk.android.app.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.core.util.DeeplinkHolder
import com.nunchuk.android.core.util.UnlockPinSourceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.walletsecurity.unlock.UnlockPinActivity
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import io.branch.indexing.BranchUniversalObject
import io.branch.referral.Branch
import io.branch.referral.BranchError
import io.branch.referral.util.LinkProperties
import io.branch.referral.validators.IntegrationValidator
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
internal class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var navigator: NunchukNavigator

    @Inject
    lateinit var deeplinkHolder: DeeplinkHolder

    private val viewModel: SplashViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        IntegrationValidator.validate(this)
        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            handleBranchDeepLink(branchUniversalObject, linkProperties, error)
        }.withData(this.intent.data).init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        Timber.tag("BranchSDK_Tester").e("onNewIntent")
        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            handleBranchDeepLink(branchUniversalObject, linkProperties, error)
        }.reInit()
    }

    private fun handleBranchDeepLink(branchUniversalObject: BranchUniversalObject?, linkProperties: LinkProperties?, error: BranchError?) {
        if (error != null) {
            Timber.tag("BranchSDK_Tester")
                .e("branch init failed. Caused by -%s", error.message)
        } else {
            Timber.tag("BranchSDK_Tester")
                .e("branch init complete! - %s, %s", branchUniversalObject, linkProperties)
            if (branchUniversalObject != null) {
                Timber.tag("BranchSDK_Tester").e("title %s", branchUniversalObject.title)
                Timber.tag("BranchSDK_Tester")
                    .e("CanonicalIdentifier %s", branchUniversalObject.canonicalIdentifier)
                Timber.tag("BranchSDK_Tester")
                    .e("metadata %s", branchUniversalObject.contentMetadata?.convertToJson())
                val metaData = branchUniversalObject.contentMetadata?.convertToJson()
                deeplinkHolder.setDeeplinkInfo(metaData.toString())
            }
            if (linkProperties != null) {
                Timber.tag("BranchSDK_Tester").e("Channel %s", linkProperties.channel)
                Timber.tag("BranchSDK_Tester")
                    .e("control params %s", linkProperties.controlParams)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransparentStatusBar()
        subscribeEvents()
    }

    private fun subscribeEvents() {
        flowObserver(viewModel.event) {
            handleEvent(it)
        }
    }

    private fun handleEvent(event: SplashEvent) {
        when (event) {
            is SplashEvent.NavSignInEvent -> {
                navigator.openSignInScreen(this, true)
                if (event.askPin) {
                    startActivity(Intent(this, UnlockPinActivity::class.java))
                } else if (event.askBiometric) {
                    navigator.openBiometricScreen(this)
                }
            }
            is SplashEvent.NavHomeScreenEvent -> {
                navigator.openMainScreen(this)
                if (NotificationUtils.areNotificationsEnabled(this).not() && !event.isGuestMode) {
                    navigator.openTurnNotificationScreen(this)
                }
                if (event.askPin) {
                    startActivity(Intent(this, UnlockPinActivity::class.java))
                } else if (event.askBiometric) {
                    navigator.openBiometricScreen(this)
                }
            }

            is SplashEvent.InitErrorEvent -> NCToastMessage(this).showError(event.error)
            SplashEvent.NavUnlockPinScreenEvent -> {
                navigator.openSignInScreen(this, false)
                navigator.openUnlockPinScreen(this, UnlockPinSourceFlow.SIGN_IN_UNKNOWN_MODE)
            }
        }
        overridePendingTransition(0, 0)
        finish()
    }

    companion object {
        fun navigate(activityContext: Context) {
            val intent = Intent(activityContext, SplashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            activityContext.startActivity(intent)
        }
    }
}

