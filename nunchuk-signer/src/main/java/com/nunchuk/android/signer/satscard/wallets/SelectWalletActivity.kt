package com.nunchuk.android.signer.satscard.wallets

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.signer.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectWalletActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.select_wallet_navigation)
        graph.setStartDestination(R.id.selectWalletFragment)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater).also {
            enableEdgeToEdge()
        }
    }

    companion object {

        private const val SLOTS_EXTRA = "slots"
        private const val TYPE_EXTRA = "type"
        private const val CLAIM_INHERITANCE_TX_PARAM_EXTRA = "claim_param"

        fun navigate(
            activity: Context,
            slots: List<SatsCardSlot>,
            type: Int,
            claimInheritanceTxParam: ClaimInheritanceTxParam?,
        ) {
            val intent = Intent(activity, SelectWalletActivity::class.java)
                .apply {
                    putExtra(SLOTS_EXTRA, slots.toTypedArray<SatsCardSlot>())
                    putExtra(TYPE_EXTRA, type)
                    putExtra(CLAIM_INHERITANCE_TX_PARAM_EXTRA, claimInheritanceTxParam)
                }
            activity.startActivity(intent)
        }
    }
}