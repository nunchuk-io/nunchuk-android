package com.nunchuk.android.main.membership.honey.distribution

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.model.inheritance.ClaimOption
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data object InheritanceKeyAddedRoute

@Serializable
private data object KeyDistributionRoute

@Serializable
private data object VerifyBackupsRoute

/** Which screen of the sub-flow the caller wants; the rest are reachable from there. */
enum class KeyDistributionEntry {
    /** Straight after the key was added: confirm it landed, then pick the sharing method. */
    KEY_ADDED,

    /** From the key row when no sharing method has been recorded yet. */
    SHARING_METHOD,

    /** From the key row of a "do both" key: the checklist of the two artifacts. */
    VERIFY_BACKUPS,
}

/**
 * The tail of adding an off-chain inheritance key: confirm the key landed, have the owner pick how
 * it reaches their Beneficiary (share the seed phrase, upload an encrypted backup, or both), and —
 * for "do both" — track the two artifacts on a checklist.
 *
 * It is its own activity for the same reason
 * [com.nunchuk.android.main.membership.backupseedphrase.BackUpSeedPhraseActivity]
 * is — the key-list screen is a fragment in an XML nav graph, and this sub-flow is Compose-only and
 * re-enterable from the key row when the choice was skipped.
 *
 * It never runs a verification itself. The flows that verify an artifact need step state the key
 * list owns (the backup file path of a TAPSIGNER or Coldcard, for one), so the checklist reports
 * back which half the owner tapped and the key list runs it.
 */
@AndroidEntryPoint
class KeyDistributionActivity : BaseComposeActivity() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: KeyDistributionViewModel by viewModels()

    private val signer by lazy { intent.parcelable<SignerModel>(EXTRA_SIGNER) }
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    private val entry by lazy {
        intent.getStringExtra(EXTRA_ENTRY)
            ?.let { name -> KeyDistributionEntry.entries.firstOrNull { it.name == name } }
            ?: KeyDistributionEntry.KEY_ADDED
    }

    /**
     * What the caller already knows the key records, so the checklist draws the right cards on its
     * first frame instead of an empty one while the server is re-read.
     */
    private val claimOptions by lazy {
        intent.getStringArrayListExtra(EXTRA_CLAIM_OPTIONS)
            .orEmpty()
            .mapNotNull { name -> ClaimOption.entries.firstOrNull { it.name == name } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val signer = signer ?: run {
            finish()
            return
        }
        viewModel.init(
            signer = signer,
            groupId = groupId,
            walletId = walletId,
            claimOptions = claimOptions,
        )

        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()
            var showRemoveBackupConfirm by rememberSaveable { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                viewModel.event.collect { event ->
                    when (event) {
                        // Every choice lands on the checklist, whether it asked for one artifact
                        // or two: that is where the owner verifies what they chose, and where
                        // they can change their mind about it.
                        is KeyDistributionEvent.Saved -> {
                            viewModel.refreshClaimState()
                            // Coming from the checklist, it is still on the stack behind the
                            // choice screen the owner just used to change their mind.
                            if (entry == KeyDistributionEntry.VERIFY_BACKUPS) {
                                if (!navController.popBackStack()) finishWithResult()
                            } else {
                                navController.navigate(VerifyBackupsRoute)
                            }
                        }

                        is KeyDistributionEvent.Error -> NCToastMessage(this@KeyDistributionActivity)
                            .showError(event.message)
                    }
                }
            }

            NunchukTheme {
                NavHost(
                    navController = navController,
                    startDestination = when (entry) {
                        KeyDistributionEntry.KEY_ADDED -> InheritanceKeyAddedRoute
                        KeyDistributionEntry.SHARING_METHOD -> KeyDistributionRoute
                        KeyDistributionEntry.VERIFY_BACKUPS -> VerifyBackupsRoute
                    },
                ) {
                    composable<InheritanceKeyAddedRoute> {
                        InheritanceKeyAddedContent(
                            signer = state.signer,
                            remainTime = remainTime,
                            onContinueClicked = { navController.navigate(KeyDistributionRoute) },
                        )
                    }
                    composable<KeyDistributionRoute> {
                        KeyDistributionContent(
                            remainTime = remainTime,
                            supportedOptions = state.supportedOptions,
                            claimNote = state.claimNote,
                            selectedChoice = state.selectedChoice,
                            isLoading = state.isLoading,
                            onChoiceSelected = viewModel::onChoiceSelected,
                            onContinueClicked = {
                                if (state.isDroppingEncryptedBackup) {
                                    showRemoveBackupConfirm = true
                                } else {
                                    viewModel.onContinueClicked()
                                }
                            },
                            // Nothing behind it when this is the start destination.
                            onBackPressed = { if (!navController.popBackStack()) finish() },
                        )
                        if (showRemoveBackupConfirm) {
                            RemoveEncryptedBackupSheet(
                                onConfirm = {
                                    showRemoveBackupConfirm = false
                                    viewModel.onContinueClicked()
                                },
                                onDismiss = { showRemoveBackupConfirm = false },
                            )
                        }
                    }
                    composable<VerifyBackupsRoute> {
                        VerifyBackupsContent(
                            remainTime = remainTime,
                            statuses = state.claimState.statuses(),
                            isContinueEnabled = state.claimState.isSettled,
                            onVerifyEncryptedBackup = { finishWithResult(ClaimOption.ENCRYPTED_BACKUP) },
                            onVerifySeedPhrase = { finishWithResult(ClaimOption.SEED_PHRASE) },
                            onChangeSharingMethod = { navController.navigate(KeyDistributionRoute) },
                            onContinueClicked = { finishWithResult() },
                            onBackPressed = { if (!navController.popBackStack()) finish() },
                        )
                    }
                }
            }
        }
    }

    /**
     * [verifyClaimOption] asks the caller to run that half's verification flow — the checklist
     * chooses, the key list runs.
     */
    private fun finishWithResult(verifyClaimOption: ClaimOption? = null) {
        setResult(
            Activity.RESULT_OK,
            Intent().apply {
                putExtra(GlobalResultKey.EXTRA_SIGNER, signer)
                verifyClaimOption?.let { putExtra(EXTRA_VERIFY_CLAIM_OPTION, it.name) }
            },
        )
        finish()
    }

    companion object {
        private const val EXTRA_SIGNER = "signer"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_ENTRY = "entry"
        private const val EXTRA_CLAIM_OPTIONS = "claim_options"

        /** Which claim option the owner asked to verify, when the flow ended on that tap. */
        const val EXTRA_VERIFY_CLAIM_OPTION = "verify_claim_option"

        /**
         * [walletId] empty targets the draft wallet, otherwise the replacement on that wallet.
         * [claimOptions] is what the caller already records for the key; it only spares the
         * checklist an empty first frame, and the server's answer replaces it.
         */
        fun buildIntent(
            activityContext: Context,
            signer: SignerModel,
            groupId: String = "",
            walletId: String = "",
            entry: KeyDistributionEntry = KeyDistributionEntry.KEY_ADDED,
            claimOptions: List<ClaimOption> = emptyList(),
        ) = Intent(activityContext, KeyDistributionActivity::class.java).apply {
            putExtra(EXTRA_SIGNER, signer)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_ENTRY, entry.name)
            putStringArrayListExtra(
                EXTRA_CLAIM_OPTIONS,
                ArrayList(claimOptions.map { it.name }),
            )
        }
    }
}
