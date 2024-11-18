package com.nunchuk.android.main.membership.byzantine.groupdashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GroupDashboardActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        navHostFragment.navController.setGraph(R.navigation.group_dashboard_navigation, intent.extras)

        if (savedInstanceState == null) {
            val message = intent.getStringExtra(MESSAGE)
            if (!message.isNullOrEmpty()) {
                NCToastMessage(this).show(message)
            }
        }
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        private const val GROUP_ID = "group_id"
        private const val WALLET_ID = "wallet_id"
        private const val MESSAGE = "message"

        fun navigate(
            activity: Context,
            groupId: String?,
            walletId: String?,
            message: String?
        ) {
            val intent = buildIntent(activity, groupId, walletId, message)
            activity.startActivity(intent)
        }

        fun buildIntent(
            activity: Context,
            groupId: String?,
            walletId: String?,
            message: String? = null
        ): Intent {
            return Intent(activity, GroupDashboardActivity::class.java)
                .putExtra(GROUP_ID, groupId)
                .putExtra(WALLET_ID, walletId)
                .putExtra(MESSAGE, message)
        }
    }
}