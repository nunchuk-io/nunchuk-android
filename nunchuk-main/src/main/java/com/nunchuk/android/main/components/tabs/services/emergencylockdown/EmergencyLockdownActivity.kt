package com.nunchuk.android.main.components.tabs.services.emergencylockdown

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgs
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.emergencylockdown.intro.EmergencyLockdownIntroFragmentArgs
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyLockdownActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.emergency_lockdown_navigation)
        graph.setStartDestination(R.id.emergencyLockdownIntroFragment)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {
        fun navigate(activity: Context, verifyToken: String) {
            activity.startActivity(Intent(activity, EmergencyLockdownActivity::class.java).apply {
                putExtras(
                    EmergencyLockdownIntroFragmentArgs(
                        verifyToken = verifyToken,
                    ).toBundle()
                )
            })
        }
    }
}