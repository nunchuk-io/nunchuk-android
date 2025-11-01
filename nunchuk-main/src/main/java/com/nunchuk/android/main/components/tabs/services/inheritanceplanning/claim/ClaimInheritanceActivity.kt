package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.BTC_SATOSHI_EXCHANGE_RATE
import com.nunchuk.android.core.util.SelectWalletType
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.addInheritanceKey
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ClaimInheritanceGraph(
                        navigator = navigator
                    )
                }
            }
        )
    }
}

@Composable
private fun ClaimInheritanceGraph(
    activityViewModel: ClaimInheritanceViewModel = hiltViewModel(),
    navigator: NunchukNavigator
) {
    val navController = rememberNavController()
    val activity = LocalActivity.current
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
                onContinue = { isUseDevice ->
                    if (isUseDevice) {
                        navController.navigateToRestoreSeedPhraseHardware()
                    } else {

                    }
                },
            )
            restoreSeedPhraseHardware(
                onBackPressed = {
                    navController.popBackStack()
                },
                onContinue = {
                    // TODO: Handle continue action
                },
            )
            claimMagicPhrase(
                onBackPressed = {
                    activity?.finish()
                },
                onContinue = { magicPhrase, initResult ->
                    navController.navigateToClaimBackupPassword(magicPhrase)
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
                        navController.navigateToClaimNote(
                            signers = signers,
                            magic = magic,
                            inheritanceAdditional = inheritanceAdditional,
                            derivationPaths = derivationPaths,
                            activityViewModel = activityViewModel
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
                    activity?.finish()
                },
            )
            noInheritancePlanFound(
                onCloseClick = {
                    activity?.finish()
                },
            )
            claimNote(
                onDoneClick = {
                    activity?.finish()
                },
                onWithdrawClick = { balance, signers, magic, derivationPaths ->
                    navController.navigateToClaimWithdrawBitcoin(
                        walletBalance = balance,
                        signers = signers,
                        magic = magic,
                        derivationPaths = derivationPaths,
                        activityViewModel = activityViewModel
                    )
                },
            )
            claimWithdrawBitcoin(
                onNavigateToInputAmount = { walletBalance, signers, magic, derivationPaths ->
                    activity?.let { act ->
                        navigator.openInputAmountScreen(
                            activityContext = act,
                            walletId = "",
                            availableAmount = walletBalance,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = signers.map { it.id },
                                magicalPhrase = magic.trim(),
                                derivationPaths = derivationPaths,
                                totalAmount = walletBalance
                            )
                        )
                    }
                },
                onNavigateToSelectWallet = { walletBalance, signers, magic, derivationPaths ->
                    activity?.let { act ->
                        navigator.openSelectWalletScreen(
                            activityContext = act,
                            slots = emptyList(),
                            type = SelectWalletType.TYPE_INHERITANCE_WALLET,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = signers.map { it.id },
                                magicalPhrase = magic.trim(),
                                derivationPaths = derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = true
                            )
                        )
                    }
                },
                onNavigateToWalletIntermediary = { walletBalance, signers, magic, derivationPaths ->
                    activity?.let { act ->
                        navigator.openWalletIntermediaryScreen(
                            activityContext = act,
                            quickWalletParam = QuickWalletParam(
                                claimInheritanceTxParam = ClaimInheritanceTxParam(
                                    masterSignerIds = signers.map { it.id },
                                    magicalPhrase = magic.trim(),
                                    derivationPaths = derivationPaths,
                                    totalAmount = walletBalance,
                                    isUseWallet = true
                                ),
                                type = SelectWalletType.TYPE_INHERITANCE_WALLET
                            )
                        )
                    }
                },
                onNavigateToAddReceipt = { walletBalance, signers, magic, derivationPaths ->
                    activity?.let { act ->
                        val sweepType = SweepType.SWEEP_TO_EXTERNAL_ADDRESS
                        val totalBalance = walletBalance * BTC_SATOSHI_EXCHANGE_RATE
                        val totalInBtc = Amount(value = totalBalance.toLong()).pureBTC()
                        navigator.openAddReceiptScreen(
                            activityContext = act,
                            walletId = "",
                            outputAmount = totalInBtc,
                            availableAmount = totalInBtc,
                            subtractFeeFromAmount = true,
                            sweepType = sweepType,
                            claimInheritanceTxParam = ClaimInheritanceTxParam(
                                masterSignerIds = signers.map { it.id },
                                magicalPhrase = magic.trim(),
                                derivationPaths = derivationPaths,
                                totalAmount = walletBalance,
                                isUseWallet = false
                            )
                        )
                    }
                },
            )
        }
    }
}
