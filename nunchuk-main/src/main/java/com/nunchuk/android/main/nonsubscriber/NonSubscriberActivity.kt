package com.nunchuk.android.main.nonsubscriber

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.main.R
import com.nunchuk.android.main.nonsubscriber.intro.NonSubscriberIntroFragmentArgs
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NonSubscriberActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        initStartDestination()
    }

    private fun initStartDestination() {
        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment)
        navHostFragment.navController.setGraph(
            R.navigation.non_subscriber_navigation,
            intent.extras
        )
    }

    companion object {
        fun start(context: Activity, bannerId: String) {
            context.startActivity(Intent(context, NonSubscriberActivity::class.java).apply {
                putExtras(NonSubscriberIntroFragmentArgs(bannerId).toBundle())
            })
        }
    }
}