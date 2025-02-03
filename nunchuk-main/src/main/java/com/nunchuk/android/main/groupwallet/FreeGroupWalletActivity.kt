package com.nunchuk.android.main.groupwallet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.main.groupwallet.join.CommonQRCodeActivity
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FreeGroupWalletActivity : BaseComposeActivity() {

    private val viewModel: FreeGroupWalletViewModel by viewModels()

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
                            startDestination = freeGroupWalletRoute,
                        ) {
                            freeGroupWallet(
                                viewModel = viewModel,
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
                                },
                                onAddExistingKey = { signer, index ->
                                    viewModel.setCurrentSignerIndex(index)
                                    navController.navigateCustomKey(signer)
                                },
                                onAddNewKey = {
                                    viewModel.setCurrentSignerIndex(it)
                                    openSignerIntro(it)
                                },
                                finishScreen = ::finish,
                                onContinueClicked = { group ->
                                    navigator.openReviewWalletScreen(
                                        activityContext = this@FreeGroupWalletActivity,
                                        args = ReviewWalletArgs(
                                            walletName = group.name,
                                            walletType = WalletType.MULTI_SIG,
                                            addressType = group.addressType,
                                            totalRequireSigns = group.m,
                                            signers = group.signers,
                                            groupId = group.id
                                        )
                                    )
                                },
                                returnToHome = {
                                    navigator.returnToMainScreen(this@FreeGroupWalletActivity)
                                }
                            )

                            customKeyNavigation(
                                viewModel = viewModel,
                                onCustomIndexDone = viewModel::addSignerToGroup
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
            groupId = viewModel.groupId,
            index = index,
            supportedSigners = viewModel.getSuggestedSigners()
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