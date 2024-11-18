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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.nunchuk.android.BuildConfig
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.settings.walletsecurity.unlock.UnlockPinActivity
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.util.setTransparentStatusBar
import dagger.hilt.android.AndroidEntryPoint
import io.branch.referral.Branch
import io.branch.referral.validators.IntegrationValidator
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: SplashViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        IntegrationValidator.validate(this)
        Branch.sessionBuilder(this).withCallback { branchUniversalObject, linkProperties, error ->
            if (BuildConfig.DEBUG) {
                if (error != null) {
                    Timber.tag("BranchSDK_Tester")
                        .e("branch init failed. Caused by -%s", error.message)
                } else {
                    Timber.tag("BranchSDK_Tester").e("branch init complete!")
                    if (branchUniversalObject != null) {
                        Timber.tag("BranchSDK_Tester").e("title %s", branchUniversalObject.title)
                        Timber.tag("BranchSDK_Tester")
                            .e("CanonicalIdentifier %s", branchUniversalObject.canonicalIdentifier)
                        Timber.tag("BranchSDK_Tester")
                            .e("metadata %s", branchUniversalObject.contentMetadata.convertToJson())
                    }
                    if (linkProperties != null) {
                        Timber.tag("BranchSDK_Tester").e("Channel %s", linkProperties.channel)
                        Timber.tag("BranchSDK_Tester")
                            .e("control params %s", linkProperties.controlParams)
                    }
                }
            }
        }.withData(this.intent.data).init()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        if (intent.hasExtra("branch_force_new_session") && intent.getBooleanExtra("branch_force_new_session",false)) {
            Branch.sessionBuilder(this).withCallback { referringParams, error ->
                if (BuildConfig.DEBUG) {
                    if (error != null) {
                        Timber.tag("BranchSDK_Tester").e(error.message)
                    } else if (referringParams != null) {
                        Timber.tag("BranchSDK_Tester").e(referringParams.toString())
                    }
                }
            }.reInit()
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
            SplashEvent.NavSignInEvent -> navigator.openSignInScreen(this, true)
            is SplashEvent.NavHomeScreenEvent -> {
                navigator.openMainScreen(this)
                if (NotificationUtils.areNotificationsEnabled(this).not()) {
                    navigator.openTurnNotificationScreen(this)
                }
                if (event.askPin) {
                    startActivity(Intent(this, UnlockPinActivity::class.java))
                }
            }

            is SplashEvent.InitErrorEvent -> NCToastMessage(this).showError(event.error)
        }
        overridePendingTransition(0, 0)
        finish()
    }
}

