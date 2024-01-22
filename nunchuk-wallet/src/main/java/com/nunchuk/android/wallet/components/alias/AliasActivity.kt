package com.nunchuk.android.wallet.components.alias

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AliasActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = setAliasRoute
                    ) {
                        setAlias(
                            walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                            onBackPress = onBackPressedDispatcher::onBackPressed
                        )
                    }
                }
            },
        )
    }

    companion object {
        const val EXTRA_WALLET_ID = "wallet_id"

        fun createIntent(context: Context, walletId: String): Intent {
            return Intent(context, AliasActivity::class.java).apply {
                putExtra(EXTRA_WALLET_ID, walletId)
            }
        }
    }
}