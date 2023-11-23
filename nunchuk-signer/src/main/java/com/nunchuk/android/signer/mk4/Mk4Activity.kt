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

package com.nunchuk.android.signer.mk4

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.share.ColdcardAction
import com.nunchuk.android.signer.R
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Mk4Activity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.mk4_navigation)
        when (intent.serializable<ColdcardAction>(EXTRA_ACTION)!!) {
            ColdcardAction.CREATE -> graph.setStartDestination(R.id.mk4InfoFragment)
            ColdcardAction.RECOVER_KEY -> graph.setStartDestination(R.id.coldcardRecoverFragment)
            ColdcardAction.RECOVER_SINGLE_SIG_WALLET,
            ColdcardAction.RECOVER_MULTI_SIG_WALLET,
            ColdcardAction.PARSE_SINGLE_SIG_WALLET,
            ColdcardAction.PARSE_MULTISIG_WALLET -> graph.setStartDestination(R.id.mk4IntroFragment)
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            WindowCompat.setDecorFitsSystemWindows(
                window,
                destination.id == R.id.addMk4NameFragment
            )
        }
    }

    val action : ColdcardAction by lazy(LazyThreadSafetyMode.NONE) {
        intent.serializable(EXTRA_ACTION)!!
    }
    val groupId : String by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    companion object {
        private const val EXTRA_IS_MEMBERSHIP_FLOW = "is_membership_flow"
        private const val EXTRA_ACTION = "action"
        private const val EXTRA_GROUP_ID = "group_id"
        fun navigate(activity: Activity, isMembershipFlow: Boolean, action: ColdcardAction, groupId: String) {
            activity.startActivity(buildIntent(activity, isMembershipFlow, action, groupId))
        }

        fun buildIntent(activity: Activity, isMembershipFlow: Boolean, action: ColdcardAction, groupId: String): Intent {
            return Intent(activity, Mk4Activity::class.java).apply {
                putExtra(EXTRA_IS_MEMBERSHIP_FLOW, isMembershipFlow)
                putExtra(EXTRA_ACTION, action)
                putExtra(EXTRA_GROUP_ID, groupId)
            }
        }
    }
}