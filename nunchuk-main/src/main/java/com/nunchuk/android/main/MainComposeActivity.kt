package com.nunchuk.android.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.main.archive.Archive
import com.nunchuk.android.main.archive.archiveScreen
import com.nunchuk.android.main.guest.GuestWalletNotice
import com.nunchuk.android.main.guest.guestWalletNoticeScreen
import com.nunchuk.android.nav.args.MainComposeArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainComposeActivity : BaseComposeActivity() {
    private val args: MainComposeArgs by lazy {
        MainComposeArgs.deserializeFrom(intent.extras ?: Bundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            val startDestination: Any = when (args.type) {
                MainComposeArgs.TYPE_ARCHIVE -> Archive
                MainComposeArgs.TYPE_GUEST_WALLET_NOTICE -> GuestWalletNotice
                else -> throw IllegalArgumentException("Unknown type: ${args.type}")
            }

            NunchukTheme {
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    archiveScreen(
                        openWalletDetail = { walletId ->
                            navigator.openWalletDetailsScreen(
                                activityContext = this@MainComposeActivity,
                                walletId = walletId
                            )
                        }
                    )
                    guestWalletNoticeScreen(onGotIt = {
                        finish()
                    })
                }
            }
        }
    }

    companion object {
        fun start(activity: Activity, args: MainComposeArgs) {
            activity.startActivity(
                Intent(activity, MainComposeActivity::class.java).apply {
                    putExtras(args.buildBundle())
                }
            )
        }
    }
}