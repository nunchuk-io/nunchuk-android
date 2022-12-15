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
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InheritancePlanningActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.inheritance_planning_navigation)
        graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
        val planFlow = intent.getIntExtra(EXTRA_INHERITANCE_PLAN_FLOW, InheritancePlanFlow.NONE)
        when (planFlow) {
            InheritancePlanFlow.SETUP -> {
                graph.setStartDestination(R.id.inheritancePlanOverviewFragment)
            }
            InheritancePlanFlow.VIEW -> {
                graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
            }
            InheritancePlanFlow.CLAIM -> {
                graph.setStartDestination(R.id.inheritanceClaimFragment)
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
                    planFlow = planFlow
                ).toBundle()
            }
            else -> null
        }
        navHostFragment.navController.setGraph(graph, bundle)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val EXTRA_INHERITANCE_PLAN_FLOW = "extra_inheritance_plan_flow"
        private const val EXTRA_VERIFY_TOKEN = "extra_verify_token"
        private const val EXTRA_INHERITANCE = "extra_inheritance"

        fun navigate(
            activity: Context,
            verifyToken: String?,
            inheritance: Inheritance?,
            @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int
        ) {
            activity.startActivity(
                Intent(activity, InheritancePlanningActivity::class.java)
                    .putExtra(EXTRA_INHERITANCE_PLAN_FLOW, flowInfo)
                    .putExtra(EXTRA_VERIFY_TOKEN, verifyToken)
                    .putExtra(EXTRA_INHERITANCE, inheritance)
            )
        }
    }
}