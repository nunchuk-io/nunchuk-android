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

package com.nunchuk.android.main.membership

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.model.isByzantine
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.components.base.BaseWalletConfigActivity
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MembershipActivity : BaseWalletConfigActivity<ActivityNavigationBinding>() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: MembershipViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (groupId.isEmpty()) {
            membershipStepManager.initStep(groupId)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.membership_navigation)
        val stage = intent.serializable<MembershipStage>(EXTRA_GROUP_STEP)
        val isCreateAssistedWallet = intent.getBooleanExtra(EXTRA_ADD_ON_HONEY_BADGER, false)
        val plan = membershipStepManager.plan
        when (stage) {
            MembershipStage.NONE -> {
                if (plan.isByzantine()) {
                    if (groupId.isNotEmpty() || isCreateAssistedWallet) {
                        graph.setStartDestination(R.id.introAssistedWalletFragment)
                    } else {
                        graph.setStartDestination(R.id.groupWalletIntroFragment)
                    }
                } else {
                    graph.setStartDestination(R.id.introAssistedWalletFragment)
                }
            }

            MembershipStage.CONFIG_RECOVER_KEY_AND_CREATE_WALLET_IN_PROGRESS, MembershipStage.SETUP_INHERITANCE ->
                graph.setStartDestination(if (groupId.isEmpty()) R.id.addKeyStepFragment else R.id.addGroupKeyStepFragment)

            MembershipStage.ADD_KEY_ONLY -> graph.setStartDestination(R.id.groupPendingIntroFragment)
            MembershipStage.REGISTER_WALLET -> if (index > 0) graph.setStartDestination(R.id.registerWalletToColdcardFragment) else graph.setStartDestination(
                R.id.registerWalletToAirgapFragment
            )

            else -> Unit
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
        observer()
    }

    private fun observer() {
        flowObserver(viewModel.state) {
            if (it.groupWalletType != null) {
                membershipStepManager.initStep(it.groupId, it.groupWalletType)
            }
        }
    }

    val groupId: String
            by lazy(LazyThreadSafetyMode.NONE) { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }

    private val index: Int
            by lazy(LazyThreadSafetyMode.NONE) { intent.getIntExtra(EXTRA_INDEX, 0) }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        const val EXTRA_GROUP_STEP = "group_step"
        const val EXTRA_KEY_WALLET_ID = "wallet_id"
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_ADD_ON_HONEY_BADGER = "add_on_honey_badger"
        private const val EXTRA_INDEX = "index"
        private const val EXTRA_AIRGAP_INDEX = "airgap_index"
        private const val EXTRA_IS_SINGLE_REGISTER = "is_single_register"

        fun buildIntent(
            activity: Activity,
            groupStep: MembershipStage,
            walletId: String? = null,
            groupId: String? = null,
            addOnHoneyBadger: Boolean = false,
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, groupStep)
            putExtra(EXTRA_KEY_WALLET_ID, walletId)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_ADD_ON_HONEY_BADGER, addOnHoneyBadger)
        }

        fun openRegisterWalletIntent(
            activity: Activity,
            walletId: String,
            groupId: String,
            index: Int,
            airgapIndex: Int,
            singleRegister: Boolean = false,
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, MembershipStage.REGISTER_WALLET)
            putExtra(EXTRA_KEY_WALLET_ID, walletId)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_ADD_ON_HONEY_BADGER, true)
            putExtra(EXTRA_INDEX, index)
            putExtra(EXTRA_AIRGAP_INDEX, airgapIndex)
            putExtra(EXTRA_IS_SINGLE_REGISTER, singleRegister)
        }
    }
}