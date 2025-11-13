package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.app.Activity
import android.os.Bundle
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.push.PushEventManager
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.signer.toModel
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.isMiniscript
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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.ClaimMagicPhraseRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.claimMagicPhrase
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.navigateToNoInheritancePlanFound
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.noInheritancePlanFound
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.InheritanceOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToPrepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToRecoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.prepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.recoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.navigateToRestoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.restoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin.claimWithdrawBitcoin
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin.navigateToClaimWithdrawBitcoin
import com.nunchuk.android.model.Amount
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.nav.args.ClaimArgs
import com.nunchuk.android.nav.args.UploadConfigurationType
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseComposeActivity() {
    private val viewModel: ClaimInheritanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val args = ClaimArgs.deserializeFrom(intent)

        if (!args.bsms.isNullOrEmpty()) {
            viewModel.getInheritanceStatus(bsms = args.bsms)
            viewModel.getClaimingWallet(bsms = args.bsms.orEmpty())
        }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ClaimInheritanceGraph(
                        args = args,
                        activity = this@ClaimInheritanceActivity,
                        navigator = navigator,
                        pushEventManager = pushEventManager,
                    )
                }
            }
        )
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
            val signers = it.data?.parcelableArrayList<SignerModel>(GlobalResultKey.EXTRA_SIGNERS)
            if (!signers.isNullOrEmpty()) {
                navController.popBackStack<ClaimMagicPhraseRoute>(false)
                activityViewModel.addSigner(signers.first())
            }
        }
    }

    val context = LocalContext.current
    val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
    val sharedUiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sharedUiState.event) {
        sharedUiState.event?.let { event ->
            when (event) {
                is ClaimInheritanceEvent.NavigateToNoInheritanceFound -> {
                    navController.navigateToNoInheritancePlanFound()
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
            }
            activityViewModel.onEventHandled()
        }
    }

    LaunchedEffect(Unit) {
        pushEventManager.event.collectLatest { event ->
            if (event is PushEvent.LocalUserSignerAdded) {
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
                    when (option) {
                        InheritanceOption.HARDWARE_DEVICE -> {
                            navigator.openSignerIntroScreen(
                                launcher = signerIntroLauncher,
                                activityContext = activity,
                                onChainAddSignerParam = OnChainAddSignerParam(
                                    flags = OnChainAddSignerParam.FLAG_ADD_INHERITANCE_SIGNER,
                                    isClaiming = true
                                )
                            )
                        }

                        InheritanceOption.SEED_PHRASE -> {
                            navController.navigateToRecoverInheritanceKey()
                        }
                    }
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
                            isClaiming = true
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
                    if (initResult.walletType.isMiniscript()) {
                        if (initResult.inheritanceKeyCount > 1) {
                            navController.navigateToAddInheritanceKey(
                                index = 1,
                                totalKeys = initResult.inheritanceKeyCount
                            )
                        } else {
                            navController.navigateToPrepareInheritanceKey()
                        }
                    } else {
                        navController.navigateToClaimBackupPassword(magicPhrase)
                    }
                },
            )
            claimBackupPassword(
                snackState = snackbarHostState,
                onBackPressed = {
                    navController.popBackStack()
                },
                onNoInheritancePlanFound = {
                    navController.navigateToNoInheritancePlanFound()
                },
                onSuccess = { signers, magic, inheritanceAdditional, derivationPaths ->
                    val bufferPeriodCountdown = inheritanceAdditional.bufferPeriodCountdown
                    if (bufferPeriodCountdown == null) {
                        activityViewModel.setClaimNoteData(
                            signers,
                            magic,
                            inheritanceAdditional,
                            derivationPaths
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
            noInheritancePlanFound(
                snackState = snackbarHostState,
                onCloseClick = {
                    activity.finish()
                },
            )
            claimNote(
                snackState = snackbarHostState,
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
            claimWithdrawBitcoin(
                snackState = snackbarHostState,
                onNavigateToInputAmount = {
                    claimData.inheritanceAdditional?.balance?.let { walletBalance ->
                        navigator.openInputAmountScreen(
                            activityContext = activity,
                            walletId = "",
                            availableAmount = walletBalance,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = claimData.requiredSigners.map { it.id },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                bsms = claimData.bsms
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
                                masterSignerIds = claimData.requiredSigners.map { it.id },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = true,
                                bsms = claimData.bsms
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
                                    masterSignerIds = claimData.requiredSigners.map { it.id },
                                    magicalPhrase = claimData.magic.trim(),
                                    derivationPaths = claimData.derivationPaths,
                                    totalAmount = walletBalance,
                                    isUseWallet = true,
                                    bsms = claimData.bsms
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
                                masterSignerIds = claimData.requiredSigners.map { it.id },
                                magicalPhrase = claimData.magic.trim(),
                                derivationPaths = claimData.derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = false,
                                bsms = claimData.bsms
                            )
                        )
                    }
                },
            )
        }
    }
}
