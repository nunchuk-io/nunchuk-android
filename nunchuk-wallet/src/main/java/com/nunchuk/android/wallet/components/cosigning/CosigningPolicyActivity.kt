package com.nunchuk.android.wallet.components.cosigning

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.KeyPolicy
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CosigningPolicyActivity : BaseActivity<ActivityNavigationBinding>() {
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
        navHostFragment.navController.setGraph(R.navigation.cosigning_policy_navigation, intent.extras)
    }

    companion object {
        fun start(activity: Activity, keyPolicy: KeyPolicy?, xfp: String) {
            activity.startActivity(Intent(activity, CosigningPolicyActivity::class.java).apply {
                putExtras(CosigningPolicyFragmentArgs(keyPolicy = keyPolicy, xfp = xfp).toBundle())
            })
        }
    }
}