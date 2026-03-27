package com.nunchuk.android.main.groupwallet

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.nunchuk.android.compose.NcSnackbarVisuals
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.manager.NcToastManager
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_NFC_TOPUP_XPUBS
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.R
import com.nunchuk.android.main.groupwallet.join.CommonQRCodeActivity
import com.nunchuk.android.main.groupwallet.keypolicies.FreeGroupKeyPoliciesRoute
import com.nunchuk.android.main.groupwallet.keypolicies.freeGroupKeyPolicies
import com.nunchuk.android.main.groupwallet.keypolicies.navigateToFreeGroupKeyPolicies
import com.nunchuk.android.main.groupwallet.recover.FreeGroupWalletRecoverRoute
import com.nunchuk.android.main.groupwallet.recover.freeGroupWalletRecover
import com.nunchuk.android.main.membership.signer.SignerIntroActivity
import com.nunchuk.android.main.membership.wallet.createWalletSuccessScreen
import com.nunchuk.android.main.membership.wallet.navigateCreateWalletSuccessScreen
import com.nunchuk.android.model.GroupSandbox
import com.nunchuk.android.model.SignatureFlowType
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.signer.SupportedSigner
import com.nunchuk.android.model.signer.supportedAirgapSigner
import com.nunchuk.android.model.signer.supportedSeverSigner
import com.nunchuk.android.nav.args.AddWalletArgs
import com.nunchuk.android.nav.args.ReviewWalletArgs
import com.nunchuk.android.signer.defaultSupportedSigners
import com.nunchuk.android.type.WalletType
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.utils.serializable
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class FreeGroupWalletActivity : BaseComposeNfcActivity(), InputBipPathBottomSheetListener {

    private val viewModel: FreeGroupWalletViewModel by viewModels()
    private val replaceWalletId by lazy { intent.getStringExtra(EXTRA_REPLACE_WALLET_ID).orEmpty() }
    private val filePath by lazy { intent.getStringExtra(EXTRA_FILE_PATH).orEmpty() }
    private val quickWalletParam by lazy { intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM) }
    private val actionType by lazy {
        intent.serializable<FreeGroupActionType>(EXTRA_ACTION_TYPE)
            ?: FreeGroupActionType.NONE
    }

    private val signerIntroLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val isPlatformKeySelected = data.getBooleanExtra(
                    SignerIntroActivity.EXTRA_PLATFORM_KEY_SELECTED, false
                )
                if (isPlatformKeySelected) {
                    val names = if (viewModel.isMiniscriptWallet()) {
                        viewModel.uiState.value.currentKeyToAssign
                            .takeIf { it.isNotEmpty() }
                            ?.let(::listOf)
                            .orEmpty()
                    } else {
                        emptyList()
                    }
                    viewModel.enableGroupPlatformKey(names)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    val snackState = remember { SnackbarHostState() }
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    Timber.d("FreeGroupWalletActivity state: $state")

                    LaunchedEffect(state.requestCacheTapSignerXpubEvent) {
                        if (state.requestCacheTapSignerXpubEvent) {
                            startNfcFlow(REQUEST_NFC_TOPUP_XPUBS, "Please rescan your TAPSIGNER to get a new XPUB")
                            viewModel.resetRequestCacheTapSignerXpub()
                        }
                    }

                    LaunchedEffect(state.isCreatedReplaceGroup) {
                        if (state.isCreatedReplaceGroup) {
                            navController.navigate(
                                FreeGroupWalletRoute,
                                navOptions {
                                    popUpTo<ReplaceWalletIntroRoute> {
                                        inclusive = true
                                    }
                                }
                            )
                            viewModel.resetReplaceGroup()
                        }
                    }

                    LaunchedEffect(state.errorMessage) {
                        if (state.errorMessage.isNotEmpty()) {
                            snackState.showSnackbar(
                                NcSnackbarVisuals(
                                    message = state.errorMessage,
                                    type = NcToastType.ERROR
                                )
                            )
                            viewModel.markMessageHandled()
                        }
                    }

                    LaunchedEffect(state.finalizeGroup) {
                        val finalizeGroup = state.finalizeGroup
                        if (finalizeGroup != null) {
                            navController.navigateCreateWalletSuccessScreen(
                                replacedWalletId = finalizeGroup.replaceWalletId,
                                walletId = finalizeGroup.walletId
                            )
                            viewModel.markFinalizeGroupHandled()
                        }
                    }

                    val startDestination: Any = when (actionType) {
                        FreeGroupActionType.RECOVER -> FreeGroupWalletRecoverRoute(
                            walletId = intent.getStringExtra(EXTRA_WALLET_ID).orEmpty(),
                            filePath = filePath,
                            qrList = intent.getStringArrayListExtra(EXTRA_QR_LIST).orEmpty(),
                        )
                        FreeGroupActionType.KEY_POLICIES -> FreeGroupKeyPoliciesRoute(
                            walletId = intent.getStringExtra(EXTRA_WALLET_ID),
                        )
                        FreeGroupActionType.NONE -> if (replaceWalletId.isEmpty()) {
                            FreeGroupWalletRoute
                        } else {
                            ReplaceWalletIntroRoute
                        }
                    }

                    NunchukTheme {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                        ) {
                            freeGroupWallet(
                                viewModel = viewModel,
                                snackState = snackState,
                                navigator = navigator,
                                onEditClicked = { groupId, hasGroupSigner, miniscriptTemplate ->
                                    navigator.openAddWalletScreen(
                                        activityContext = this@FreeGroupWalletActivity,
                                        args = AddWalletArgs(
                                            decoyPin = "",
                                            groupWalletId = groupId,
                                            hasGroupSigner = hasGroupSigner,
                                            miniscriptTemplate = miniscriptTemplate
                                        )
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
                                        groupId = viewModel.groupId,
                                        supportedSigners = viewModel.getSuggestedSigners()
                                    )
                                },
                                finishScreen = ::finish,
                                onContinueClicked = { group ->
                                    if (group.replaceWalletId.isNotEmpty()) {
                                        viewModel.finalizeGroup(group)
                                    } else if (group.walletType == WalletType.MINISCRIPT) {
                                        // For MINISCRIPT wallets, always use openReviewWalletScreen
                                        val signerMap = viewModel.getWalletSigners()
                                            .associate { it.fingerPrint to it.name }
                                        val state = viewModel.uiState.value
                                        
                                        // Convert namedSigners Map to list of NamedSigner objects
                                        val namedSignersList = state.namedSigners.map { (keyName, signer) ->
                                            com.nunchuk.android.nav.args.NamedSigner(keyName, signer)
                                        }
                                        
                                        navigator.openReviewWalletScreen(
                                            activityContext = this@FreeGroupWalletActivity,
                                            args = ReviewWalletArgs(
                                                walletName = group.name,
                                                walletType = WalletType.MINISCRIPT,
                                                addressType = group.addressType,
                                                totalRequireSigns = group.m,
                                                signers = group.signers.map {
                                                    it.copy(name = signerMap[it.masterFingerprint].orEmpty())
                                                },
                                                groupId = group.id,
                                                quickWalletParam = quickWalletParam,
                                                scriptNode = state.scriptNode,
                                                scriptNodeMuSig = state.scriptNodeMuSig,
                                                keyPath = state.keyPath,
                                                namedSigners = namedSignersList,
                                                supportedTypes = state.supportedTypes
                                            )
                                        )
                                    } else if (group.addressType.isTaproot()) {
                                        navigator.openTaprootScreen(
                                            activityContext = this@FreeGroupWalletActivity,
                                            walletName = group.name,
                                            walletType = WalletType.MULTI_SIG,
                                            addressType = group.addressType,
                                            groupSandboxId = viewModel.groupId,
                                            quickWalletParam = quickWalletParam
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
                                                groupId = group.id,
                                                quickWalletParam = quickWalletParam
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
                                    if (viewModel.isMiniscriptWallet()) {
                                        if (index != -1) {
                                            viewModel.setCurrentSignerIndex(index)
                                        }
                                    } else {
                                        viewModel.setCurrentSignerIndex(index)
                                    }
                                    InputBipPathBottomSheet.show(
                                        supportFragmentManager,
                                        signer.id,
                                        signer.derivationPath
                                    )
                                },
                                openWalletDetail = {
                                    navigator.returnToMainScreen(this@FreeGroupWalletActivity)
                                    NcToastManager.scheduleShowMessage(getString(R.string.nc_the_group_wallet_has_been_created))
                                    navigator.openWalletDetailsScreen(
                                        activityContext = this@FreeGroupWalletActivity,
                                        walletId = it
                                    )
                                },
                                refresh = viewModel::getGroupSandbox,
                                onAddNewKeyForMiniscript = { supportedSigners ->
                                    openSignerIntro(
                                        groupId = viewModel.groupId,
                                        supportedSigners = supportedSigners
                                    )
                                },
                                onStartAddKeyForMiniscript = { keyName ->
                                    viewModel.setCurrentKeyToAssign(keyName)
                                    viewModel.setSlotOccupied(true)
                                },
                                onConfigPlatformKey = {
                                    navController.navigateToFreeGroupKeyPolicies()
                                }
                            )

                            freeGroupKeyPolicies(
                                onBackClicked = {
                                    if (!navController.popBackStack()) {
                                        finish()
                                    }
                                },
                                onSaveSuccess = { groupSandbox ->
                                    viewModel.onPlatformKeyPoliciesUpdated(groupSandbox)
                                },
                                onUpdatePolicySuccess = ::finish,
                                onOpenWalletAuthentication = { walletId, dummyTransaction ->
                                    navigator.openWalletAuthentication(
                                        walletId = walletId,
                                        requiredSignatures = dummyTransaction?.requiredSignatures ?: 0,
                                        type = VerificationType.SIGN_DUMMY_TX,
                                        activityContext = this@FreeGroupWalletActivity,
                                        dummyTransactionId = dummyTransaction?.id,
                                        signatureFlowType = SignatureFlowType.FREE_GROUP_WALLET,
                                    )
                                },
                            )

                            customKeyNavigation(
                                viewModel = viewModel,
                                onCustomIndexDone = viewModel::addSignerToGroup
                            )

                            replaceWalletIntroNavigation(
                                viewModel = viewModel,
                                onContinueClicked = viewModel::createReplaceGroup,
                                snackState = snackState
                            )

                            freeGroupWalletRecover(
                                navigator = navigator,
                                onAddNewKey = { walletId, supportedSigners ->
                                    openSignerIntro(
                                        groupId = walletId,
                                        supportedSigners = supportedSigners
                                    )
                                },
                                openSignerIntro = { walletId, supportedSigners ->
                                    openSignerIntro(
                                        groupId = walletId,
                                        supportedSigners = supportedSigners
                                    )
                                },
                                finishScreen = ::finish,
                                onOpenWalletDetail = {
                                    navigateToSelectWallet(
                                        navigator = navigator,
                                        quickWalletParam = quickWalletParam
                                    ) {
                                        navigator.openWalletDetailsScreen(
                                            activityContext = this@FreeGroupWalletActivity,
                                            walletId = it
                                        )
                                    }
                                }
                            )

                            createWalletSuccessScreen(
                                onBackPress = {
                                    navigateToSelectWallet(
                                        navigator = navigator,
                                        quickWalletParam = quickWalletParam
                                    ) {
                                        returnToMainScreen()
                                    }
                                },
                                onContinueClicked = {
                                    navigateToSelectWallet(
                                        navigator = navigator,
                                        quickWalletParam = quickWalletParam
                                    ) {
                                        val group = state.group
                                        val wallet = viewModel.getWallet()
                                        checkWalletBalance(group, wallet)
                                    }
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

    private fun checkWalletBalance(group: GroupSandbox?, wallet: Wallet?) {
        if (group != null && wallet != null) {
            if (wallet.balance.pureBTC() == 0.0) {
                returnToMainScreen()
            } else {
                NCInfoDialog(this@FreeGroupWalletActivity)
                    .showDialog(
                        title = getString(R.string.nc_confirmation),
                        message = getString(R.string.nc_transfer_fund_desc),
                        btnYes = getString(R.string.nc_yes_do_it_now),
                        btnInfo = getString(R.string.nc_i_ll_do_it_later),
                        onYesClick = {
                            navigator.openRollOverWalletScreen(
                                activityContext = this@FreeGroupWalletActivity,
                                oldWalletId = group.replaceWalletId,
                                newWalletId = group.walletId,
                                startScreen = RollOverWalletFlow.REFUND,
                                source = RollOverWalletSource.REPLACE_KEY
                            )
                        },
                        onInfoClick = {
                            returnToMainScreen()
                            navigator.openWalletDetailsScreen(
                                activityContext = this@FreeGroupWalletActivity,
                                walletId = group.walletId
                            )
                        }
                    )
            }
        }
    }

    private fun returnToMainScreen() = lifecycleScope.launch {
        navigator.returnToMainScreen(this@FreeGroupWalletActivity)
        pushEventManager.push(PushEvent.CloseWalletDetail)
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        viewModel.changeBip32Path(masterSignerId, newInput)
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.isMiniscriptWallet()) return
        viewModel.setSlotOccupied(false)
    }

    private fun openSignerIntro(
        groupId: String,
        supportedSigners: List<SupportedSigner>
    ) {
        val newSupportedSigners = supportedSigners.ifEmpty { defaultSupportedSigners } + supportedSeverSigner + supportedAirgapSigner
        navigator.openSignerIntroScreen(
            launcher = signerIntroLauncher,
            activityContext = this,
            groupId = groupId,
            supportedSigners = newSupportedSigners
        )
    }

    companion object {
        const val EXTRA_GROUP_ID = "group_id"
        const val EXTRA_WALLET_ID = "wallet_id"
        const val EXTRA_REPLACE_WALLET_ID = "replace_wallet_id"
        const val EXTRA_FILE_PATH = "file_path"
        const val EXTRA_QUICK_WALLET_PARAM = "quick_wallet_param"
        const val EXTRA_QR_LIST = "qr_list"
        const val EXTRA_ACTION_TYPE = "action_type"

        /**
         * Start [FreeGroupWalletActivity] with [groupId] and [walletId]
         * @param context [Context]
         * @param groupId [String] group id
         * @param walletId [String] wallet id only pass when replace wallet from Wallet Config Screen
         */
        fun start(
            context: Context,
            groupId: String? = null,
            walletId: String? = null,
            quickWalletParam: QuickWalletParam? = null
        ) {
            context.startActivity(Intent(context, FreeGroupWalletActivity::class.java).apply {
                putExtra(EXTRA_GROUP_ID, groupId)
                putExtra(EXTRA_REPLACE_WALLET_ID, walletId)
                putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
            })
        }

        fun startRecover(
            context: Context,
            walletId: String,
            filePath: String = "",
            qrList: List<String> = emptyList(),
            quickWalletParam: QuickWalletParam? = null
        ) {
            context.startActivity(
                Intent(
                    context,
                    FreeGroupWalletActivity::class.java
                ).apply {
                    putExtra(EXTRA_WALLET_ID, walletId)
                    putExtra(EXTRA_FILE_PATH, filePath)
                    putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
                    putExtra(EXTRA_QR_LIST, ArrayList(qrList))
                    putExtra(EXTRA_ACTION_TYPE, FreeGroupActionType.RECOVER)
                })
        }

        fun startKeyPolicies(
            context: Context,
            walletId: String,
        ) {
            context.startActivity(Intent(context, FreeGroupWalletActivity::class.java).apply {
                putExtra(EXTRA_WALLET_ID, walletId)
                putExtra(EXTRA_ACTION_TYPE, FreeGroupActionType.KEY_POLICIES)
            })
        }
    }
}
