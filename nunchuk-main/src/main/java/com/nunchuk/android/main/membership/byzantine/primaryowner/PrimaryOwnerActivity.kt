package com.nunchuk.android.main.membership.byzantine.primaryowner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.util.PrimaryOwnerFlow
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.share.membership.MembershipFragment
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class PrimaryOwnerActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                .putExtra(MembershipFragment.EXTRA_GROUP_ID, groupId)
            activity.startActivity(intent)
        }
    }
}