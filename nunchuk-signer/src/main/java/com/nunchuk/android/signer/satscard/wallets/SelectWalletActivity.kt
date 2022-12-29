package com.nunchuk.android.signer.satscard.wallets

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectWalletActivity : BaseNfcActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHost.navController.navInflater
        val graph = inflater.inflate(R.navigation.select_wallet_navigation)
        graph.setStartDestination(R.id.selectWalletFragment)
        navHost.navController.setGraph(graph, intent.extras)
    }

    companion object {
        fun navigate(
            activity: Activity,
            slots: Array<SatsCardSlot> = emptyArray(),
            type: Int,
            walletBalance: Float = 0.0F,
            masterSignerId: String = "",
            magicalPhrase: String = ""
        ) {
            activity.startActivity(Intent(activity, SelectWalletActivity::class.java).apply {
                putExtras(
                    SelectWalletFragmentArgs(
                        slots = slots,
                        type = type,
                        walletBalance = walletBalance,
                        masterSignerId = masterSignerId,
                        magicalPhrase = magicalPhrase
                    ).toBundle()
                )
            })
        }
    }
}