package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.addkey.addInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.claimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.backuppassword.navigateToClaimBackupPassword
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.claimBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod.navigateToClaimBufferPeriod
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.ClaimMagicPhraseRoute
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.noInheritancePlanFound
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound.navigateToNoInheritancePlanFound
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.magicphrase.claimMagicPhrase
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.InheritanceOption
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToPrepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.navigateToRecoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.prepareInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover.recoverInheritanceKey
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.navigateToRestoreSeedPhraseHardware
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware.restoreSeedPhraseHardware
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ClaimInheritanceActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    ClaimInheritanceGraph()
                }
            }
        )
    }
}

@Composable
private fun ClaimInheritanceGraph() {
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
                        // Navigate to InheritanceClaimNoteFragment
                        // This fragment is in inheritance_planning_navigation.xml graph
                        // Required arguments:
                        // - signers: Array<SignerModel>
                        // - magic: String
                        // - inheritance_additional: InheritanceAdditional
                        // - derivation_paths: Array<String>
                        // TODO: Implement navigation to InheritanceClaimNoteFragment
                        // Options:
                        // 1. Use Navigation deep link to navigate to the fragment
                        // 2. Launch a new Activity that hosts the fragment
                        // 3. Migrate the fragment to Compose and add to this NavHost
                        activity?.let { act ->
                            // Placeholder: Will be implemented in next step
                            // NavigationComponentExtensions.findNavController(act, R.id.nav_host)
                            //     .navigate(InheritanceClaimInputFragmentDirections
                            //         .actionInheritanceClaimInputFragmentToInheritanceClaimNoteFragment(
                            //             signers = signers.toTypedArray(),
                            //             magic = magic,
                            //             inheritanceAdditional = inheritanceAdditional,
                            //             derivationPaths = derivationPaths.toTypedArray()
                            //         )
                            //     )
                        }
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
        }
    }
}
