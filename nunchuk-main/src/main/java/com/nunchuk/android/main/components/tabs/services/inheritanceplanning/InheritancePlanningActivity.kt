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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan.InheritanceReviewPlanFragmentArgs
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritancePlanningActivity : BaseActivity<ActivityNavigationBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    val isOpenFromWizard: Boolean by lazy { intent.getBooleanExtra(EXTRA_IS_OPEN_FROM_WIZARD, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        membershipStepManager.setCurrentStep(MembershipStep.SETUP_INHERITANCE)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.inheritance_planning_navigation)
        graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
        val planFlow = intent.getIntExtra(EXTRA_INHERITANCE_PLAN_FLOW, InheritancePlanFlow.NONE)
        when (planFlow) {
            InheritancePlanFlow.SETUP -> {
                graph.setStartDestination(R.id.inheritanceSetupIntroFragment)
            }
            InheritancePlanFlow.VIEW -> {
                graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
            }
            InheritancePlanFlow.CLAIM -> {
                graph.setStartDestination(R.id.inheritanceClaimInputFragment)
            }
        }
        val bundle = when (planFlow) {
            InheritancePlanFlow.VIEW -> {
                val inheritance = intent.parcelable<Inheritance>(EXTRA_INHERITANCE) ?: return
                InheritanceReviewPlanFragmentArgs(
                    activationDate = inheritance.activationTimeMilis,
                    emails = inheritance.notificationEmails.toTypedArray(),
                    isNotify = inheritance.notificationEmails.isNotEmpty(),
                    magicalPhrase = inheritance.magic,
                    note = inheritance.note,
                    verifyToken = intent.getStringExtra(EXTRA_VERIFY_TOKEN).orEmpty(),
                    planFlow = planFlow,
                    bufferPeriod = inheritance.bufferPeriod,
                    walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty()
                ).toBundle()
            }
            else -> intent.extras
        }
        navHostFragment.navController.setGraph(graph, bundle)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.selectWalletFragment -> WindowCompat.setDecorFitsSystemWindows(window, true)
                else -> WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val EXTRA_INHERITANCE_PLAN_FLOW = "extra_inheritance_plan_flow"
        private const val EXTRA_VERIFY_TOKEN = "extra_verify_token"
        private const val EXTRA_INHERITANCE = "extra_inheritance"
        private const val EXTRA_IS_OPEN_FROM_WIZARD = "extra_is_open_from_wizard"
        private const val EXTRA_WALLET_ID = "wallet_id"

        fun navigate(
            walletId: String,
            activity: Context,
            verifyToken: String?,
            inheritance: Inheritance?,
            @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
            isOpenFromWizard: Boolean
        ) {
            val intent = Intent(activity, InheritancePlanningActivity::class.java)
                .putExtra(EXTRA_INHERITANCE_PLAN_FLOW, flowInfo)
                .putExtra(EXTRA_VERIFY_TOKEN, verifyToken)
                .putExtra(EXTRA_INHERITANCE, inheritance)
                .putExtra(EXTRA_IS_OPEN_FROM_WIZARD, isOpenFromWizard)
                .putExtra(EXTRA_WALLET_ID, walletId)
            activity.startActivity(intent)
        }
    }
}