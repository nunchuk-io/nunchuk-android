package com.nunchuk.android.main.groupwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.main.groupwallet.join.CommonQRCodeActivity
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FreeGroupWalletActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val groupId = intent.getStringExtra(EXTRA_GROUP_ID)
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = "free_group_wallet/$groupId",
                        ) {
                            freeGroupWallet(
                                groupId = groupId,
                                onEditClicked = {
                                    navigator.openAddWalletScreen(
                                        activityContext = this@FreeGroupWalletActivity,
                                        decoyPin = "",
                                        groupWalletId = it
                                    )
                                },
                                onShowQRCodeClicked = {
                                    CommonQRCodeActivity.start(this@FreeGroupWalletActivity, it)
                                },
                                onCopyLinkClicked = {
                                    copyToClipboard(label = "Nunchuk", text = it)
                                    NCToastMessage(this@FreeGroupWalletActivity).show("Link copied to clipboard")
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        fun start(context: Context, groupId: String? = null) {
            context.startActivity(Intent(context, FreeGroupWalletActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
            })
        }
    }
}