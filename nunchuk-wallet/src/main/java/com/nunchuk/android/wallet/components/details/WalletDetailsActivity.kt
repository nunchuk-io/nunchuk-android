package com.nunchuk.android.wallet.components.details

import android.content.Context
import android.os.Bundle
import androidx.navigation.findNavController
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.databinding.ActivityWalletDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletDetailsActivity : BaseActivity<ActivityWalletDetailBinding>() {

    override fun initializeBinding() = ActivityWalletDetailBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findNavController(R.id.nav_host).setGraph(R.navigation.wallet_detail_navigation, intent.extras)
    }

    companion object {
        fun start(activityContext: Context, walletId: String) {
            activityContext.startActivity(WalletDetailsArgs(walletId = walletId).buildIntent(activityContext))
        }
    }
}