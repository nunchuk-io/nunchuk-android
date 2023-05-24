package com.nunchuk.android.transaction.components.send.batchtransaction

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.amount.InputAmountArgs
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BatchTransactionActivity : BaseActivity<ActivityNavigationBinding>() {
    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation_batch_transaction)
        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    companion object {
        fun buildIntent(
            context: Context, roomId: String = "",
            walletId: String,
            availableAmount: Double,
            inputs: List<UnspentOutput> = emptyList()
        ) =
            Intent(context, BatchTransactionActivity::class.java).apply {
                putExtra("room_id", roomId)
                putExtra("wallet_id", walletId)
                putExtra("available_amount", availableAmount.toFloat())
                putExtra("unspent_outputs", inputs.toTypedArray())
            }
    }
}