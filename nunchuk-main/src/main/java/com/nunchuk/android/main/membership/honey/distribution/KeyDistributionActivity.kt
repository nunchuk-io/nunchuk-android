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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data object InheritanceKeyAddedRoute

@Serializable
private data object KeyDistributionRoute

/**
 * The tail of adding an off-chain inheritance key: confirm the key landed, then have the owner
 * pick how it reaches their Beneficiary (share the seed phrase, upload an encrypted backup, or
 * both).
 *
 * It is its own activity for the same reason
 * [com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase.BackUpSeedPhraseActivity]
 * is — the key-list screen is a fragment in an XML nav graph, and this sub-flow is Compose-only and
 * re-enterable from the key row when the choice was skipped.
 */
@AndroidEntryPoint
class KeyDistributionActivity : BaseComposeActivity() {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: KeyDistributionViewModel by viewModels()

    private val signer by lazy { intent.parcelable<SignerModel>(EXTRA_SIGNER) }
    private val groupId by lazy { intent.getStringExtra(EXTRA_GROUP_ID).orEmpty() }
    private val walletId by lazy { intent.getStringExtra(EXTRA_WALLET_ID).orEmpty() }

    /**
     * "Inheritance key added" confirms an add that just happened. Coming back from the key row to
     * pick a sharing method there is nothing to confirm, so that screen is skipped.
     */
    private val skipKeyAdded by lazy { intent.getBooleanExtra(EXTRA_SKIP_KEY_ADDED, false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val signer = signer ?: run {
            finish()
            return
        }
        viewModel.init(signer = signer, groupId = groupId, walletId = walletId)

        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val remainTime by membershipStepManager.remainingTime.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.event.collect { event ->
                    when (event) {
                        is KeyDistributionEvent.Saved -> {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }

                        is KeyDistributionEvent.Error -> NCToastMessage(this@KeyDistributionActivity)
                            .showError(event.message)
                    }
                }
            }

            NunchukTheme {
                NavHost(
                    navController = navController,
                    startDestination = if (skipKeyAdded) {
                        KeyDistributionRoute
                    } else {
                        InheritanceKeyAddedRoute
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
                            onContinueClicked = viewModel::onContinueClicked,
                            // Nothing behind it when this is the start destination.
                            onBackPressed = { if (!navController.popBackStack()) finish() },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_SIGNER = "signer"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_WALLET_ID = "wallet_id"
        private const val EXTRA_SKIP_KEY_ADDED = "skip_key_added"

        /** [walletId] empty targets the draft wallet, otherwise the replacement on that wallet. */
        fun buildIntent(
            activityContext: Context,
            signer: SignerModel,
            groupId: String = "",
            walletId: String = "",
            skipKeyAdded: Boolean = false,
        ) = Intent(activityContext, KeyDistributionActivity::class.java).apply {
            putExtra(EXTRA_SIGNER, signer)
            putExtra(EXTRA_GROUP_ID, groupId)
            putExtra(EXTRA_WALLET_ID, walletId)
            putExtra(EXTRA_SKIP_KEY_ADDED, skipKeyAdded)
        }
    }
}
