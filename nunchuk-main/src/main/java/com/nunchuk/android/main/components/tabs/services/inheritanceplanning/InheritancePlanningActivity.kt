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

package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseShareSaveFileActivity
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.byzantine.GroupWalletType
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class InheritancePlanningActivity : BaseShareSaveFileActivity<ActivityNavigationBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    private val viewModel by viewModels<InheritancePlanningViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val groupId = intent.getStringExtra(MembershipFragment.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isEmpty()) {
            membershipStepManager.initStep("", GroupWalletType.TWO_OF_FOUR_MULTISIG)
        }
        membershipStepManager.setCurrentStep(MembershipStep.SETUP_INHERITANCE)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.inheritance_planning_navigation)
        graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
        val planFlow = intent.getIntExtra(EXTRA_INHERITANCE_PLAN_FLOW, InheritancePlanFlow.NONE)
        when (planFlow) {
            InheritancePlanFlow.SETUP -> graph.setStartDestination(R.id.inheritanceSetupIntroFragment)
            InheritancePlanFlow.VIEW -> graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
            InheritancePlanFlow.SIGN_DUMMY_TX -> graph.setStartDestination(R.id.inheritanceReviewPlanGroupGroupFragment)
            InheritancePlanFlow.REQUEST -> graph.setStartDestination(R.id.inheritanceRequestPlanningConfirmFragment)
        }
        when (planFlow) {
            InheritancePlanFlow.SETUP -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        planFlow = planFlow,
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                        sourceFlow = intent.getIntExtra(EXTRA_SOURCE_FLOW, InheritanceSourceFlow.NONE)
                    )
                )
            }

            InheritancePlanFlow.VIEW -> {
                val inheritance = intent.parcelable<Inheritance>(EXTRA_INHERITANCE) ?: return
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        activationDate = inheritance.activationTimeMilis,
                        emails = inheritance.notificationEmails,
                        isNotify = inheritance.notificationEmails.isNotEmpty(),
                        magicalPhrase = inheritance.magic,
                        note = inheritance.note,
                        verifyToken = intent.getStringExtra(EXTRA_VERIFY_TOKEN).orEmpty(),
                        planFlow = planFlow,
                        bufferPeriod = inheritance.bufferPeriod,
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        sourceFlow = intent.getIntExtra(EXTRA_SOURCE_FLOW, InheritanceSourceFlow.NONE),
                        groupId = groupId,
                        dummyTransactionId = intent.getStringExtra(EXTRA_DUMMY_TRANSACTION_ID)
                            .orEmpty(),
                        notificationSettings = inheritance.notificationPreferences,
                        inheritanceKeys = inheritance.inheritanceKeys.map { it.xfp },
                        selectedZoneId = inheritance.timezone
                    )
                )
            }
            InheritancePlanFlow.SIGN_DUMMY_TX -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        verifyToken = intent.getStringExtra(EXTRA_VERIFY_TOKEN).orEmpty(),
                        planFlow = planFlow,
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                        dummyTransactionId = intent.getStringExtra(EXTRA_DUMMY_TRANSACTION_ID)
                            .orEmpty()
                    )
                )
            }
            InheritancePlanFlow.REQUEST -> {
                viewModel.setOrUpdate(
                    InheritancePlanningParam.SetupOrReview(
                        walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                        groupId = groupId,
                    )
                )
            }
        }
        navHostFragment.navController.setGraph(graph, intent.extras)
        navHostFragment.navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.selectWalletFragment -> WindowCompat.setDecorFitsSystemWindows(window, true)
                else -> WindowCompat.setDecorFitsSystemWindows(window, false)
            }
        }
        observer()
        observeEvent()
    }

    fun showSaveShareOption() {
        super.showSaveShareOption(false)
    }

    override fun shareFile() {
        viewModel.handleShareBsms()
    }

    override fun saveFileToLocal() {
        viewModel.saveBSMSToLocal()
    }

    private fun observer() {
        flowObserver(viewModel.state) {
            if (it.groupWalletType != null) {
                membershipStepManager.initStep(it.groupId, it.groupWalletType)
            }
        }
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            handleEvent(event)
        }
    }

    private fun handleEvent(event: InheritancePlanningEvent) {
        when (event) {
            is InheritancePlanningEvent.Success -> shareFile(event)
            is InheritancePlanningEvent.Failure -> NCToastMessage(this).showWarning(event.message)
            is InheritancePlanningEvent.SaveLocalFile -> {
                showSaveFileState(event.isSuccess)
            }
        }
    }

    private fun shareFile(event: InheritancePlanningEvent.Success) {
        controller.shareFile(event.filePath)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }
    }

    companion object {

        private const val EXTRA_INHERITANCE_PLAN_FLOW = "extra_inheritance_plan_flow"
        private const val EXTRA_VERIFY_TOKEN = "extra_verify_token"
        private const val EXTRA_INHERITANCE = "extra_inheritance"
        private const val EXTRA_SOURCE_FLOW = "extra_source_flow"
        const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_DUMMY_TRANSACTION_ID = "dummy_transaction_id"

        const val RESULT_REQUEST_PLANNING = "result_request_planning"

        fun navigate(
            launcher: ActivityResultLauncher<Intent>? = null,
            walletId: String,
            activity: Context,
            verifyToken: String?,
            inheritance: Inheritance?,
            @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int,
            @InheritanceSourceFlow.InheritanceSourceFlowInfo sourceFlow: Int,
            groupId: String?,
            dummyTransactionId: String?
        ) {
            val intent = Intent(activity, InheritancePlanningActivity::class.java)
                .putExtra(EXTRA_INHERITANCE_PLAN_FLOW, flowInfo)
                .putExtra(EXTRA_VERIFY_TOKEN, verifyToken)
                .putExtra(EXTRA_INHERITANCE, inheritance)
                .putExtra(EXTRA_SOURCE_FLOW, sourceFlow)
                .putExtra(EXTRA_WALLET_ID, walletId)
                .putExtra(MembershipFragment.EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_DUMMY_TRANSACTION_ID, dummyTransactionId)
            if (launcher != null) {
                launcher.launch(intent)
            } else {
                activity.startActivity(intent)
            }
        }
    }
}