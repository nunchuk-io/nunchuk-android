package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.network.ApiErrorCode.INHERITANCE_PLAN_NOT_ACTIVE
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.R
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.addInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.navigateToAddInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.claimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.navigateToClaimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.claimBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.navigateToClaimBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote.ClaimNoteRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote.claimNote
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.claimnote.navigateToClaimNote
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete.exportComplete
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.exportcomplete.navigateToExportComplete
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.ClaimMagicPhraseRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.claimMagicPhrase
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.inheritanceError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.navigateToInheritanceError
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.InheritanceOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToPrepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToRecoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.prepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.recoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.navigateToRestoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.restoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.navigateToVerifyInheritanceMessage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.verifymessage.verifyInheritanceMessage
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin.claimWithdrawBitcoin
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin.navigateToClaimWithdrawBitcoin
import com.nunchuk.android.model.Amount
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.ClaimArgs
import com.nunchuk.android.nav.args.UploadConfigurationType
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseNfcActivity<ViewBinding>() {
    private val viewModel: ClaimInheritanceViewModel by viewModels()

    override fun initializeBinding(): ViewBinding = ViewBinding {
        val args = ClaimArgs.deserializeFrom(intent)

        if (!args.bsms.isNullOrEmpty()) {
            viewModel.getInheritanceStatus(bsms = args.bsms)
            viewModel.getClaimingWallet(bsms = args.bsms.orEmpty())
        }
        ComposeView(this).apply {
            setContent {
                ClaimInheritanceGraph(
                    args = args,
                    activity = this@ClaimInheritanceActivity,
                    navigator = navigator,
                    pushEventManager = pushEventManager,
                    activityViewModel = viewModel
                )
            }
        }
    }.also {
        enableEdgeToEdge()
    }
}

@Composable
private fun ClaimInheritanceGraph(
    args: ClaimArgs,
    activity: Activity,
    activityViewModel: ClaimInheritanceViewModel = hiltViewModel(),
    navigator: NunchukNavigator,
    pushEventManager: PushEventManager,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val startScreen = if (args.bsms.isNullOrEmpty()) {
        ClaimMagicPhraseRoute
    } else {
        ClaimNoteRoute
    }

    val importSeedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val mnemonic = it.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            val passphrase = it.data?.getStringExtra(GlobalResultKey.PASSPHRASE).orEmpty()
            if (mnemonic.isNotEmpty()) {
                navController.popBackStack<ClaimMagicPhraseRoute>(false)
                activityViewModel.createSoftwareSignerFromMnemonic(mnemonic, passphrase)
            }
        }
    }

    val signerIntroLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val signer = it.data?.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
            if (signer != null) {
                navController.popBackStack<ClaimMagicPhraseRoute>(false)
                activityViewModel.addSigner(signer)
            }
        }
    }

    val context = LocalContext.current
    val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
    val sharedUiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        if (claimData.magic.isNotEmpty()) {
            activityViewModel.checkRequestedAddDesktopKey()
        }
        onPauseOrDispose { }
    }

    LaunchedEffect(sharedUiState.event) {
        sharedUiState.event?.let { event ->
            when (event) {
                is ClaimInheritanceEvent.NavigateToInheritanceError -> {
                    val title = if (event.errorCode == INHERITANCE_PLAN_NOT_ACTIVE) {
                        context.getString(R.string.nc_inheritance_error_title)
                    } else {
                        context.getString(R.string.nc_no_inheritance_plan_found)
                    }
                    val message = if (event.errorCode == INHERITANCE_PLAN_NOT_ACTIVE) {
                        event.message
                    } else {
                        context.getString(R.string.nc_no_inheritance_plan_found_desc)
                    }
                    navController.navigateToInheritanceError(
                        title = title,
                        message = message
                    )
                }

                is ClaimInheritanceEvent.AddMoreSigners -> {
                    navController.navigateToAddInheritanceKey(
                        index = claimData.signers.size.inc(),
                        totalKeys = claimData.requiredKeyCount
                    )
                }

                is ClaimInheritanceEvent.KeyAlreadyAdded -> {
                    navController.navigateToAddInheritanceKey(
                        index = claimData.signers.size.inc(),
                        totalKeys = claimData.requiredKeyCount,
                    )
                    snackbarHostState.showNunchukSnackbar(
                        message = context.getString(R.string.nc_error_add_same_key),
                        type = NcToastType.ERROR
                    )
                }

                is ClaimInheritanceEvent.ShowError -> {
                    snackbarHostState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }

                ClaimInheritanceEvent.SignerAdded -> navController.popBackStack<ClaimMagicPhraseRoute>(
                    false
                )

                is ClaimInheritanceEvent.SignMessage -> {
                    navController.navigateToVerifyInheritanceMessage()
                }

                is ClaimInheritanceEvent.GenerateChallengeSuccess -> {
                    when(event.option) {
                        InheritanceOption.HARDWARE_DEVICE -> {
                            val flags = if (claimData.isOnChainClaim) {
                                OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER
                            } else {
                                OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER or OnChainAddSignerParam.FLAG_ADD_INHERITANCE_OFF_CHAIN_SIGNER
                            }
                            navigator.openSignerIntroScreen(
                                launcher = signerIntroLauncher,
                                activityContext = activity,
                                onChainAddSignerParam = OnChainAddSignerParam(
                                    flags = flags,
                                    magic = claimData.magic
                                )
                            )
                        }
                        InheritanceOption.SEED_PHRASE -> {
                            if (claimData.isOnChainClaim) {
                                navController.navigateToRecoverInheritanceKey()
                            } else {
                                navController.navigateToClaimBackupPassword()
                            }
                        }
                    }
                }

                ClaimInheritanceEvent.ImportFile -> return@LaunchedEffect
            }
            activityViewModel.onEventHandled()
        }
    }

    LaunchedEffect(Unit) {
        pushEventManager.event.collectLatest { event ->
            if (event is PushEvent.ClaimSignerAdded) {
                val signer = event.signer
                navController.popBackStack<ClaimMagicPhraseRoute>(false)
                activityViewModel.addSigner(signer.toModel())
            }
        }
    }

    LaunchedEffect(claimData.inheritanceAdditional) {
        val inheritanceAdditional = claimData.inheritanceAdditional
        val isShowingClaimNote =
            navController.currentDestination?.hasRoute<ClaimNoteRoute>() == true
        if (inheritanceAdditional != null && !isShowingClaimNote) {
            navController.navigateToClaimNote()
        }
    }

    NunchukTheme {
        if (sharedUiState.isLoading) {
            NcLoadingDialog()
        }
        NavHost(
            navController = navController,
            startDestination = startScreen
        ) {
            addInheritanceKey(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onAddKeyClick = {
                    navController.navigateToPrepareInheritanceKey()
                },
            )
            prepareInheritanceKey(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = { option ->
                    activityViewModel.generateClaimSigningChallengeIfNeeded(option)
                },
            )
            recoverInheritanceKey(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = { isUseHardwareDevice ->
                    if (isUseHardwareDevice) {
                        navController.navigateToRestoreSeedPhraseHardware()
                    } else {
                        navigator.openRecoverSeedScreen(
                            launcher = importSeedLauncher,
                            activityContext = activity,
                            keyFlow = KeyFlow.ADD_AND_RETURN_PASSPHRASE
                        )
                    }
                },
            )
            restoreSeedPhraseHardware(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = {
                    navigator.openSignerIntroScreen(
                        launcher = signerIntroLauncher,
                        activityContext = activity,
                        onChainAddSignerParam = OnChainAddSignerParam(
                            flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
                            magic = claimData.magic
                        )
                    )
                },
            )
            claimMagicPhrase(
                snackState = snackbarHostState,
                onBackPressed = {
                    activity.finish()
                },
                onContinue = { magicPhrase, initResult ->
                    activityViewModel.updateClaimInit(magicPhrase, initResult)
                    if (initResult.inheritanceKeyCount > 1) {
                        navController.navigateToAddInheritanceKey(
                            index = 1,
                            totalKeys = initResult.inheritanceKeyCount
                        )
                    } else {
                        navController.navigateToPrepareInheritanceKey()
                    }
                },
            )
            claimBackupPassword(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onNoInheritancePlanFound = {
                    navController.navigateToInheritanceError(
                        title = context.getString(R.string.nc_no_inheritance_plan_found),
                        message = context.getString(R.string.nc_no_inheritance_plan_found_desc)
                    )
                },
                onSignersFromBackup = { signers ->
                    navController.popBackStack()
                    signers.forEach { activityViewModel.addSigner(it) }
                },
                onSuccess = { signers, magic, inheritanceAdditional ->
                    val bufferPeriodCountdown = inheritanceAdditional.bufferPeriodCountdown
                    if (bufferPeriodCountdown == null) {
                        activityViewModel.setClaimNoteData(
                            signers,
                            magic,
                            inheritanceAdditional,
                        )
                    } else {
                        navController.navigateToClaimBufferPeriod(bufferPeriodCountdown)
                    }
                },
            )
            claimBufferPeriod(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onGotItClick = {
                    activity.finish()
                },
            )
            inheritanceError(
                snackState = snackbarHostState,
                onCloseClick = {
                    activityViewModel.reset()
                    navController.popBackStack<ClaimMagicPhraseRoute>(false)
                },
            )
            claimNote(
                snackState = snackbarHostState,
                onViewWallet = {
                    navigator.openWalletDetailsScreen(
                        activityContext = context,
                        walletId = sharedUiState.walletId
                    )
                },
                onDoneClick = {
                    activity.finish()
                },
                onWithdrawClick = {
                    navController.navigateToClaimWithdrawBitcoin()
                    if (sharedUiState.isRequiredRegister) {
                        navigator.openUploadConfigurationScreen(
                            activityContext = context,
                            walletId = sharedUiState.walletId,
                            type = UploadConfigurationType.RegisterOnly
                        )
                    }
                },
            )
            verifyInheritanceMessage(
                snackState = snackbarHostState,
                navigator = navigator,
                onBackPressed = {
                    navController.popBackStack()
                },
                addMoreSigner = {
                    navController.navigateToAddInheritanceKey(
                        index = claimData.signers.size.inc(),
                        totalKeys = claimData.requiredKeyCount
                    )
                },
                onNavigateToExportComplete = {
                    navController.navigateToExportComplete()
                }
            )
            exportComplete(
                onImportSignature = {
                    navController.popBackStack()
                    activityViewModel.showImportFile()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
            claimWithdrawBitcoin(
                snackState = snackbarHostState,
                onNavigateToInputAmount = {
                    claimData.inheritanceAdditional?.balance?.let { walletBalance ->
                        navigator.openInputAmountScreen(
                            activityContext = activity,
                            walletId = "",
                            availableAmount = walletBalance,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = claimData.requiredSigners.map { it.fingerPrint },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                bsms = claimData.bsms,
                                signatures = claimData.signatures,
                                messageId = claimData.challenge?.id
                            )
                        )
                    }
                },
                onNavigateToSelectWallet = {
                    claimData.inheritanceAdditional?.balance?.let { walletBalance ->
                        navigator.openSelectWalletScreen(
                            activityContext = activity,
                            slots = emptyList(),
                            type = SelectWalletType.TYPE_INHERITANCE_WALLET,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = claimData.requiredSigners.map { it.fingerPrint },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = true,
                                bsms = claimData.bsms,
                                signatures = claimData.signatures,
                                messageId = claimData.challenge?.id
                            )
                        )
                    }
                },
                onNavigateToWalletIntermediary = {
                    claimData.inheritanceAdditional?.balance?.let { walletBalance ->
                        navigator.openWalletIntermediaryScreen(
                            activityContext = activity,
                            quickWalletParam = QuickWalletParam(
                                claimInheritanceTxParam = ClaimInheritanceTxParam(
                                    masterSignerIds = claimData.requiredSigners.map { it.fingerPrint },
                                    magicalPhrase = claimData.magic.trim(),
                                    derivationPaths = claimData.derivationPaths,
                                    totalAmount = walletBalance,
                                    isUseWallet = true,
                                    bsms = claimData.bsms,
                                    signatures = claimData.signatures,
                                    messageId = claimData.challenge?.id
                                ),
                                type = SelectWalletType.TYPE_INHERITANCE_WALLET
                            )
                        )
                    }
                },
                onNavigateToAddReceipt = {
                    claimData.inheritanceAdditional?.balance?.let { walletBalance ->
                        val sweepType = SweepType.SWEEP_TO_EXTERNAL_ADDRESS
                        val totalBalance = walletBalance * BTC_SATOSHI_EXCHANGE_RATE
                        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
                        navigator.openAddReceiptScreen(
                            activityContext = activity,
                            walletId = "",
                            outputAmount = totalInBtc,
                            availableAmount = totalInBtc,
                            subtractFeeFromAmount = true,
                            sweepType = sweepType,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = claimData.requiredSigners.map { it.fingerPrint },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = false,
                                bsms = claimData.bsms,
                                signatures = claimData.signatures,
                                messageId = claimData.challenge?.id
                            )
                        )
                    }
                },
            )
        }
    }
}
