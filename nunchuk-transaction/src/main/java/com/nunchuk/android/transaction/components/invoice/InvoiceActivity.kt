package com.nunchuk.android.transaction.components.invoice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.transaction.R
import com.nunchuk.android.widget.databinding.ActivityNavigationBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class InvoiceActivity : BaseActivity<ActivityNavigationBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.navigation_invoice)

        navHostFragment.navController.setGraph(graph, intent.extras)
    }

    override fun initializeBinding(): ActivityNavigationBinding {
        return ActivityNavigationBinding.inflate(layoutInflater)
    }

    companion object {

        const val EXTRA_INVOICE_INFO = "EXTRA_INVOICE_INFO"

        fun navigate(
            activity: Context,
            info: InvoiceInfo,
        ) {
            val intent = Intent(activity, InvoiceActivity::class.java)
                .putExtra(EXTRA_INVOICE_INFO, info)
            activity.startActivity(intent)
        }
    }
}