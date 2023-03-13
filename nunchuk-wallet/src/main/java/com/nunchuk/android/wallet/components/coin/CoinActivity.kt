package com.nunchuk.android.wallet.components.coin

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CoinActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding = ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.coin_navigation)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }
}