package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
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
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.addInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.navigateToAddInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.claimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.navigateToClaimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.claimBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.navigateToClaimBufferPeriod
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
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelableArrayList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ClaimInheritanceGraph(
                        activity = this@ClaimInheritanceActivity,
                        navigator = navigator,
                        pushEventManager = pushEventManager
                    )
                }
            }
        )
    }
}

@Composable
private fun ClaimInheritanceGraph(
    activity: Activity,
    activityViewModel: ClaimInheritanceViewModel = hiltViewModel(),
    navigator: NunchukNavigator,
    pushEventManager: PushEventManager
) {
    val navController = rememberNavController()
    val importSeedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val mnemonic = it.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            if (mnemonic.isNotEmpty()) {
                navController.popBackStack<ClaimMagicPhraseRoute>(false)
                activityViewModel.createSoftwareSignerFromMnemonic(mnemonic)
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

    val claimData by activityViewModel.claimData.collectAsStateWithLifecycle()
    val sharedUiState by activityViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sharedUiState.isInheritanceNotFound) {
        if (sharedUiState.isInheritanceNotFound) {
            navController.navigateToNoInheritancePlanFound()
            activityViewModel.handledInheritanceNotFound()
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
        if (inheritanceAdditional != null) {
            navController.navigateToClaimNote()
        }
    }
    LaunchedEffect(sharedUiState.addMoreSigners) {
        if (sharedUiState.addMoreSigners) {
            navController.navigateToAddInheritanceKey(
                index = claimData.signers.size.inc(),
                totalKeys = claimData.requiredKeyCount
            )
            activityViewModel.handledAddMoreSigners()
        }
    }

    NunchukTheme {
        NavHost(
            navController = navController,
            startDestination = ClaimMagicPhraseRoute
        ) {
            addInheritanceKey(
                onBackPressed = {
                    navController.popBackStack()
                },
                onAddKeyClick = {
                    navController.navigateToPrepareInheritanceKey()
                },
            )
            prepareInheritanceKey(
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
                            keyFlow = KeyFlow.ADD_AND_RETURN
                        )
                    }
                },
            )
            restoreSeedPhraseHardware(
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
                sharedViewModel = activityViewModel,
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
                onBackPressed = {
                    navController.popBackStack()
                },
                onGotItClick = {
                    activity.finish()
                },
            )
            noInheritancePlanFound(
                onCloseClick = {
                    activity.finish()
                },
            )
            claimNote(
                onDoneClick = {
                    activity.finish()
                },
                onWithdrawClick = {
                    navController.navigateToClaimWithdrawBitcoin()
                },
            )
            claimWithdrawBitcoin(
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
