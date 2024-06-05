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
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipFragment.Companion.EXTRA_GROUP_ID
import com.nunchuk.android.share.membership.MembershipFragment.Companion.EXTRA_IS_PERSONAL_WALLET
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

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.membership_navigation)
        val stage = intent.serializable<MembershipStage>(EXTRA_GROUP_STEP)
        val isPersonalWallet = intent.getBooleanExtra(EXTRA_IS_PERSONAL_WALLET, false)
        val walletType = intent.serializable<GroupWalletType>(MembershipFragment.EXTRA_WALLET_TYPE)
        if (walletType != null) {
            membershipStepManager.initStep(groupId, walletType)
        }
        when {
            stage == MembershipStage.TRANSFER_FUNDS -> graph.setStartDestination(R.id.transferFundFragment)
            stage == MembershipStage.REPLACE_KEY -> graph.setStartDestination(R.id.replaceKeyIntroFragment)
            stage == MembershipStage.ADD_KEY_ONLY -> graph.setStartDestination(R.id.groupPendingIntroFragment)
            stage == MembershipStage.REGISTER_WALLET -> graph.setStartDestination(R.id.registerWalletToAirgapFragment)
            walletType == null && isPersonalWallet -> graph.setStartDestination(R.id.selectGroupFragment)
            walletType == null && !isPersonalWallet -> graph.setStartDestination(R.id.groupWalletIntroFragment)
            groupId.isEmpty() && stage == MembershipStage.NONE -> graph.setStartDestination(R.id.introAssistedWalletFragment)
            groupId.isEmpty() && stage != MembershipStage.NONE -> graph.setStartDestination(R.id.addKeyStepFragment)
            groupId.isNotEmpty() -> graph.setStartDestination(R.id.addGroupKeyStepFragment)

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

    val walletId: String
            by lazy(LazyThreadSafetyMode.NONE) { intent.getStringExtra(EXTRA_KEY_WALLET_ID).orEmpty() }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        const val EXTRA_GROUP_STEP = "group_step"
        const val EXTRA_KEY_WALLET_ID = "wallet_id"
        const val EXTRA_REPLACED_WALLET_ID = "replaced_wallet_id"

        fun buildIntent(
            activity: Activity,
            groupStep: MembershipStage,
            walletType: GroupWalletType?,
            isPersonalWallet: Boolean,
            walletId: String? = null,
            groupId: String? = null,
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, groupStep)
            putExtra(EXTRA_KEY_WALLET_ID, walletId)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_IS_PERSONAL_WALLET, isPersonalWallet)
            putExtra(MembershipFragment.EXTRA_WALLET_TYPE, walletType)
        }

        fun openRegisterWalletIntent(
            activity: Activity,
            walletId: String,
            groupId: String,
        ) = Intent(activity, MembershipActivity::class.java).apply {
            putExtra(EXTRA_GROUP_STEP, MembershipStage.REGISTER_WALLET)
            putExtra(EXTRA_KEY_WALLET_ID, walletId)
            putExtra(EXTRA_GROUP_ID, groupId)
        }

        fun openTransferFund(
            activity: Activity,
            walletId: String,
            replacedWalletId: String,
        ) {
            activity.startActivity(
                Intent(activity, MembershipActivity::class.java).apply {
                    putExtra(EXTRA_GROUP_STEP, MembershipStage.TRANSFER_FUNDS)
                    putExtra(EXTRA_KEY_WALLET_ID, walletId)
                    putExtra(EXTRA_REPLACED_WALLET_ID, replacedWalletId)
                }
            )
        }
    }
}