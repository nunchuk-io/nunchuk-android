package com.nunchuk.android.main.groupwallet

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.join.CommonQRCodeActivity
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.widget.NCToastMessage
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class FreeGroupWalletActivity : BaseComposeNfcActivity(), InputBipPathBottomSheetListener {

    private val viewModel: FreeGroupWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(state.requestCacheTapSignerXpubEvent) {
                        if (state.requestCacheTapSignerXpubEvent) {
                            handleCacheXpub()
                            viewModel.resetRequestCacheTapSignerXpub()
                        }
                    }

                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = freeGroupWalletRoute,
                        ) {
                            freeGroupWallet(
                                viewModel = viewModel,
                                onEditClicked = { groupId, hasGroupSigner ->
                                    navigator.openAddWalletScreen(
                                        activityContext = this@FreeGroupWalletActivity,
                                        decoyPin = "",
                                        groupWalletId = groupId,
                                        hasGroupSigner = hasGroupSigner
                                    )
                                },
                                onShowQRCodeClicked = {
                                    CommonQRCodeActivity.start(this@FreeGroupWalletActivity, it)
                                },
                                onCopyLinkClicked = {
                                    copyToClipboard(label = "Nunchuk", text = it)
                                    NCToastMessage(this@FreeGroupWalletActivity).show("Link copied to clipboard")
                                },
                                onAddExistingKey = { signer, _ ->
                                    viewModel.addExistingSigner(signer)
                                },
                                onAddNewKey = {
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
                                },
                                onStartAddKey = {
                                    viewModel.setCurrentSignerIndex(it)
                                    viewModel.setSlotOccupied(true)
                                },
                                onChangeBip32Path = { index, signer ->
                                    viewModel.setCurrentSigner(signer)
                                    viewModel.setCurrentSignerIndex(index)
                                    InputBipPathBottomSheet.show(
                                        supportFragmentManager,
                                        signer.id,
                                        signer.derivationPath
                                    )
                                },
                                refresh = viewModel::getGroupSandbox
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

        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            viewModel.cacheTapSignerXpub(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
            )
            nfcViewModel.clearScanInfo()
        }
    }

    private fun handleCacheXpub() {
        NCWarningDialog(this).showDialog(
            title = getString(R.string.nc_text_info),
            message = getString(R.string.nc_new_xpub_need),
            btnYes = getString(R.string.nc_ok),
            btnNo = getString(R.string.nc_cancel),
            onYesClick = {
                startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            },
        )
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        viewModel.changeBip32Path(masterSignerId, newInput)
    }

    override fun onResume() {
        super.onResume()
        viewModel.setSlotOccupied(false)
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