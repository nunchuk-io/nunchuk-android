package com.nunchuk.android.settings.localcurrency

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.settings.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LocalCurrencyActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun initializeBinding() = ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setLightStatusBar()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.local_currency_setting_nav)
        navHostFragment.navController.graph = graph
    }

    companion object {

        fun start(activityContext: Context) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    LocalCurrencyActivity::class.java
                )
            )
        }
    }
}