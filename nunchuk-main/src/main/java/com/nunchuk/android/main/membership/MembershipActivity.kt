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

package com.nunchuk.android.main.membership

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.server.setting.ConfigureServerKeySettingFragmentArgs
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MembershipActivity : BaseWalletConfigActivity<ActivityNavigationBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.membership_navigation)
        val stage = intent.serializable<MembershipStage>(EXTRA_GROUP_STEP)
        when (stage) {
            MembershipStage.NONE -> graph.setStartDestination(R.id.introAssistedWalletFragment)
            MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS, MembershipStage.SETUP_INHERITANCE -> graph.setStartDestination(
                R.id.addKeyStepFragment
            )
            MembershipStage.CONFIG_SERVER_KEY -> graph.setStartDestination(R.id.configureServerKeySettingFragment)
            MembershipStage.CONFIG_SPENDING_LIMIT -> graph.setStartDestination(R.id.configSpendingLimitFragment)
            else -> Unit
        }
        val bundle = when (stage) {
            MembershipStage.CONFIG_SPENDING_LIMIT,
            MembershipStage.CONFIG_SERVER_KEY -> ConfigureServerKeySettingFragmentArgs(
                keyPolicy = intent.parcelable(EXTRA_KEY_POLICY),
                xfp = intent.getStringExtra(EXTRA_KEY_XFP)
            ).toBundle()
            else -> intent.extras
        }
        navHostFragment.navController.setGraph(graph, bundle)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        const val EXTRA_GROUP_STEP = "group_step"
        private const val EXTRA_KEY_POLICY = "key_policy"
        private const val EXTRA_KEY_XFP = "key_xfp"

        fun buildIntent(
            activity: Activity,
            groupStep: MembershipStage,
            keyPolicy: KeyPolicy? = null,
            xfp: String? = null
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, groupStep)
            putExtra(EXTRA_KEY_POLICY, keyPolicy)
            putExtra(EXTRA_KEY_XFP, xfp)
        }
    }
}