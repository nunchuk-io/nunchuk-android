package com.nunchuk.android.main.components.tabs.services.inheritanceplanning

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.utils.serializable
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
        when(intent.getIntExtra(EXTRA_INHERITANCE_PLAN_FLOW, InheritancePlanFlow.NONE)) {
            InheritancePlanFlow.SETUP -> {
                graph.setStartDestination(R.id.inheritancePlanOverviewFragment)
            }
            InheritancePlanFlow.VIEW -> {
                graph.setStartDestination(R.id.inheritanceReviewPlanFragment)
            }
        }
        navHostFragment.navController.graph = graph
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val EXTRA_INHERITANCE_PLAN_FLOW = "extra_inheritance_plan_flow"

        fun navigate(
            activity: Context,
            @InheritancePlanFlow.InheritancePlanFlowInfo flowInfo: Int
        ) {
            activity.startActivity(
                Intent(activity, InheritancePlanningActivity::class.java)
                    .putExtra(EXTRA_INHERITANCE_PLAN_FLOW, flowInfo)
            )
        }
    }
}