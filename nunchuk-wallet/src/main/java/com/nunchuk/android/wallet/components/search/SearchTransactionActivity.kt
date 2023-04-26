package com.nunchuk.android.wallet.components.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.wallet.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import com.nunchuk.android.widget.util.setLightStatusBar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchTransactionActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun initializeBinding() = ActivityNavigationBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        setLightStatusBar()
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navHostFragment.navController.setGraph(
            R.navigation.search_transaction_navigation,
            intent.extras
        )
    }

    companion object {
        fun start(activityContext: Context, walletId: String, roomId: String) {
            activityContext.startActivity(
                Intent(
                    activityContext,
                    SearchTransactionActivity::class.java
                ).apply {
                    putExtras(
                        SearchTransactionFragmentArgs(
                            walletId = walletId,
                            roomId = roomId
                        ).toBundle()
                    )
                }
            )
        }
    }
}