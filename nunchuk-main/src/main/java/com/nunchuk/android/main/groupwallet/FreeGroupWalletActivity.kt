package com.nunchuk.android.main.groupwallet

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.data.model.WalletConfigViewOnlyDataComposer
import com.nunchuk.android.core.data.model.getWalletConfigTypeBy
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.util.ADD_WALLET_RESULT
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.join.CommonQRCodeActivity
import com.nunchuk.android.main.groupwallet.recover.FreeGroupWalletRecoverViewModel
import com.nunchuk.android.main.groupwallet.recover.freeGroupWalletRecover
import com.nunchuk.android.model.signer.SupportedSigner
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
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }
    private val recoverViewModel: FreeGroupWalletRecoverViewModel by viewModels()

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val data = it.data
            if (it.resultCode == Activity.RESULT_OK && data != null) {
                val updatedWalletName = data.getStringExtra(ADD_WALLET_RESULT) ?: ""
                recoverViewModel.updateWalletName(updatedWalletName)
            }
        }

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

                    LaunchedEffect(state.isCreatedReplaceGroup) {
                        if (state.isCreatedReplaceGroup) {
                            navController.navigate(freeGroupWalletRoute)
                            viewModel.resetReplaceGroup()
                        }
                    }

                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = if (walletId.isEmpty()) freeGroupWalletRoute else replaceWalletIntroRoute,
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
                                    if (it.isEmpty()) return@freeGroupWallet
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
                                    openSignerIntro(
                                        index = it,
                                        groupId = viewModel.groupId,
                                        supportedSigners = viewModel.getSuggestedSigners()
                                    )
                                },
                                finishScreen = ::finish,
                                onContinueClicked = { group ->
                                    if (group.addressType.isTaproot()) {
                                        navigator.openTaprootScreen(
                                            activityContext = this@FreeGroupWalletActivity,
                                            walletName = group.name,
                                            walletType = WalletType.MULTI_SIG,
                                            addressType = group.addressType,
                                            groupSandboxId = viewModel.groupId,
                                        )
                                    } else {
                                        val signerMap = viewModel.getWalletSigners()
                                            .associate { it.fingerPrint to it.name }
                                        navigator.openReviewWalletScreen(
                                            activityContext = this@FreeGroupWalletActivity,
                                            args = ReviewWalletArgs(
                                                walletName = group.name,
                                                walletType = WalletType.MULTI_SIG,
                                                addressType = group.addressType,
                                                totalRequireSigns = group.m,
                                                signers = group.signers.map {
                                                    it.copy(name = signerMap[it.masterFingerprint].orEmpty())
                                                },
                                                groupId = group.id
                                            )
                                        )
                                    }
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

                            replaceWalletIntroNavigation(
                                onContinueClicked = viewModel::createReplaceGroup
                            )

                            freeGroupWalletRecover(
                                viewModel = recoverViewModel,
                                onAddNewKey = {
                                    openSignerIntro(
                                        index = -1,
                                        groupId = recoverViewModel.walletId,
                                        supportedSigners = recoverViewModel.getSuggestedSigners()
                                    )
                                },
                                finishScreen = ::finish,
                                onEditClicked = {
                                    recoverViewModel.getWallet()?.let { wallet ->
                                        navigator.openAddWalletScreen(
                                            activityContext = this@FreeGroupWalletActivity,
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
                                        activityContext = this@FreeGroupWalletActivity,
                                        walletId = it
                                    )
                                }
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

    private fun openSignerIntro(index: Int, groupId: String, supportedSigners: List<SupportedSigner>) {
        navigator.openSignerIntroScreen(
            activityContext = this,
            groupId = groupId,
            index = index,
            supportedSigners = supportedSigners
        )
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_WALLET_ID = "wallet_id"

        /**
         * Start [FreeGroupWalletActivity] with [groupId] and [walletId]
         * @param context [Context]
         * @param groupId [String] group id
         * @param walletId [String] wallet id only pass when replace wallet from Wallet Config Screen
         */
        fun start(
            context: Context,
            groupId: String? = null,
            walletId: String? = null
        ) {
            context.startActivity(Intent(context, FreeGroupWalletActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_WALLET_ID, walletId)
            })
        }

        const val EXTRA_WALLET_ID = "wallet_id"
        const val EXTRA_FILE_PATH = "file_path"
        fun startRecover(
            context: Context, walletId: String, filePath: String
        ) {
            context.startActivity(
                Intent(
                    context,
                    FreeGroupWalletActivity::class.java
                ).apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                    putExtra(EXTRA_FILE_PATH, filePath)
                })
        }
    }
}