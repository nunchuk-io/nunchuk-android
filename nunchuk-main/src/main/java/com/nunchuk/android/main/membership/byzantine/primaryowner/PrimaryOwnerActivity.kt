package com.nunchuk.android.main.membership.byzantine.primaryowner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.qr.DynamicQRCodeArgs.Companion.EXTRA_WALLET_ID
import com.nunchuk.android.core.util.InheritancePlanFlow
import com.nunchuk.android.core.util.InheritanceSourceFlow
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningParam
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.InheritancePlanningViewModel
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.main.membership.byzantine.groupdashboard.action.AlertActionIntroFragment.Companion.EXTRA_DUMMY_TRANSACTION_ID
import com.nunchuk.android.model.Inheritance
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class PrimaryOwnerActivity : BaseActivity<ActivityNavigationBinding>() {

    @Inject
    internal lateinit var membershipStepManager: MembershipStepManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val groupId = intent.getStringExtra(MembershipActivity.EXTRA_GROUP_ID).orEmpty()
        if (groupId.isEmpty()) {
            membershipStepManager.initStep(groupId)
        }
        membershipStepManager.setCurrentStep(MembershipStep.PRIMARY_OWNER)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.primary_owner_navigation)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        private const val EXTRA_FLOW = "flow"

        fun navigate(
            walletId: String,
            activity: Context,
            @PrimaryOwnerFlow.PrimaryOwnerFlowInfo flowInfo: Int,
            groupId: String?,
        ) {
            val intent = Intent(activity, PrimaryOwnerActivity::class.java)
                .putExtra(EXTRA_FLOW, flowInfo)
                .putExtra(MembershipActivity.EXTRA_KEY_WALLET_ID, walletId)
                .putExtra(MembershipActivity.EXTRA_GROUP_ID, groupId)
            activity.startActivity(intent)
        }
    }
}