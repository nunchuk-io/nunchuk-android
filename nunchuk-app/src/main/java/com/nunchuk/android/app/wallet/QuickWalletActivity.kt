package com.nunchuk.android.app.wallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.nunchuk.android.R
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.databinding.ActivityQuickWalletBinding
import com.nunchuk.android.wallet.personal.components.WalletIntermediaryFragmentArgs
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuickWalletActivity : BaseActivity<ActivityQuickWalletBinding>() {
    override fun initializeBinding(): ActivityQuickWalletBinding {
        return ActivityQuickWalletBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLightStatusBar()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.findNavController().setGraph(R.navigation.quick_wallet_navigation, WalletIntermediaryFragmentArgs(isQuickWallet = true).toBundle())
    }

    companion object {
        fun start(activityContext: Context) {
            activityContext.startActivity(Intent(activityContext, QuickWalletActivity::class.java))
        }
    }
}