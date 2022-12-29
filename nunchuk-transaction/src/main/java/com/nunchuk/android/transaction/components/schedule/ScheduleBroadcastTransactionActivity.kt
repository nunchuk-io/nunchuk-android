package com.nunchuk.android.transaction.components.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScheduleBroadcastTransactionActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation_schedule_broadcast)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        const val EXTRA_SCHEDULE_BROADCAST_TIME = "a"
        fun buildIntent(context: Context, walletId: String, transactionId: String) =
            Intent(context, ScheduleBroadcastTransactionActivity::class.java).apply {
                putExtras(
                    ScheduleBroadcastTransactionFragmentArgs(
                        walletId,
                        transactionId
                    ).toBundle()
                )
            }
    }
}