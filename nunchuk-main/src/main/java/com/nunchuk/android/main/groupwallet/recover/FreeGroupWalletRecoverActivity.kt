package com.nunchuk.android.main.groupwallet.recover

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.WalletConfigViewOnlyDataComposer
import com.nunchuk.android.core.data.model.getWalletConfigTypeBy
import com.nunchuk.android.core.util.ADD_WALLET_RESULT
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FreeGroupWalletRecoverActivity : BaseComposeActivity() {

    private val viewModel: FreeGroupWalletRecoverViewModel by viewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val updatedWalletName = data.getStringExtra(ADD_WALLET_RESULT) ?: ""
                viewModel.updateWalletName(updatedWalletName)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = freeGroupWalletRecoverRoute,
                        ) {
                            freeGroupWalletRecover(
                                viewModel = viewModel,
                                onAddNewKey = {
                                    openSignerIntro(it)
                                },
                                finishScreen = ::finish,
                                onEditClicked = {
                                    viewModel.getWallet()?.let { wallet ->
                                        navigator.openAddWalletScreen(
                                            activityContext = this@FreeGroupWalletRecoverActivity,
                                            decoyPin = "",
                                            launcher = launcher,
                                            walletConfigViewOnlyDataComposer = WalletConfigViewOnlyDataComposer(
                                                walletName = wallet.name,
                                                addressType = wallet.addressType,
                                                requireKeys = wallet.totalRequireSigns,
                                                totalKeys = wallet.signers.size,
                                                walletConfigType = getWalletConfigTypeBy(
                                                    wallet.signers.size,
                                                    wallet.totalRequireSigns
                                                )
                                            )
                                        )
                                    }
                                },
                                onOpenWalletDetail = {
                                    navigator.openWalletDetailsScreen(
                                        activityContext = this@FreeGroupWalletRecoverActivity,
                                        walletId = it
                                    )
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun openSignerIntro(index: Int) {
        navigator.openSignerIntroScreen(
            activityContext = this,
            groupId = viewModel.walletId, // fake data to trick the navigation
            index = -1,
            supportedSigners = viewModel.getSuggestedSigners()
        )
    }

    companion object {
        const val EXTRA_WALLET_ID = "wallet_id"
        const val EXTRA_FILE_PATH = "file_path"
        fun start(
            context: Context, walletId: String, filePath: String
        ) {
            context.startActivity(
                Intent(
                    context,
                    FreeGroupWalletRecoverActivity::class.java
                ).apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                    putExtra(EXTRA_FILE_PATH, filePath)
                })
        }
    }
}