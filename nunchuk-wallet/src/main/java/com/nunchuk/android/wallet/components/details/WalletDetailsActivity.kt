package com.nunchuk.android.wallet.components.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ActivityWalletDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletDetailsActivity : BaseActivity<ActivityWalletDetailBinding>() {

    override fun initializeBinding() = ActivityWalletDetailBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(R.navigation.wallet_detail_navigation, intent.extras)
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    WalletDetailsActivity::class.java
                ).apply {
                    putExtras(
                        WalletDetailsFragmentArgs(
                            walletId = walletId
                        ).toBundle()
                    )
                }
            )
        }
    }
}