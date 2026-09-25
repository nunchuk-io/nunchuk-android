package com.nunchuk.android.main.membership.backupseedphrase

import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.R
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.sheet.BottomSheetOption
import com.nunchuk.android.core.sheet.BottomSheetOptionListener
import com.nunchuk.android.core.sheet.SheetOption
import com.nunchuk.android.core.sheet.SheetOptionType
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.share.membership.MembershipStepManager
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BackUpSeedPhraseActivity : BaseComposeActivity(), BottomSheetOptionListener {

    @Inject
    lateinit var membershipStepManager: MembershipStepManager

    private val viewModel: BackUpSeedPhraseSharedViewModel by viewModels()

    private val args: BackUpSeedPhraseArgs by lazy {
        BackUpSeedPhraseArgs.deserializeFrom(intent)
    }

    /**
     * The last step is re-adding the key from the restored device, which opens that device's own
     * add-key flow directly — the key being verified already says which device it is.
     * What comes back differs per device, so the comparison is settled here:
     *
     * - Ledger and BitBox read the device against the expected fingerprint themselves and hand
     *   back [GlobalResultKey.EXTRA_VERIFIED_XFP] on a match, or a stand-in key naming the device
     *   on a mismatch — no key is ever created.
     * - Coldcard and air-gap compare too, and on a match mark the key verified on their own
     *   screens; on a mismatch they return the key they read.
     * - TAPSIGNER does no comparison at all and returns whatever it read, match or not.
     *
     * So a returned key means nothing on its own — it has to be checked against the key being
     * verified. A match is relayed up as a verified fingerprint, because marking the key verified
     * belongs to whoever started this flow; a mismatch is answered here, since nobody upstream
     * acts on a failed verification and the way out is to enter the words again.
     */
    private val signerIntroLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        // The device's add-key flow opens straight from the steps screen, with no key-type picker
        // in between, so backing out of it lands back on the steps rather than leaving the flow.
        if (result.resultCode == Activity.RESULT_CANCELED) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) {
            setResult(result.resultCode, data)
            finish()
            return@registerForActivityResult
        }
        val expectedXfp = args.signer?.fingerPrint
        val reAdded = data?.parcelable<SignerModel>(GlobalResultKey.EXTRA_SIGNER)
        val isMatch = !expectedXfp.isNullOrEmpty() &&
                reAdded?.fingerPrint?.equals(expectedXfp, ignoreCase = true) == true
        val verifiedXfp = data?.getStringExtra(GlobalResultKey.EXTRA_VERIFIED_XFP)
            ?: expectedXfp.takeIf { isMatch }

        // Only claim a mismatch when there was something to compare against.
        if (verifiedXfp.isNullOrEmpty() && reAdded != null && !expectedXfp.isNullOrEmpty()) {
            viewModel.onReAddedKeyMismatched(reAdded)
            return@registerForActivityResult
        }
        setResult(
            Activity.RESULT_OK,
            data?.apply {
                if (!verifiedXfp.isNullOrEmpty()) {
                    putExtra(GlobalResultKey.EXTRA_VERIFIED_XFP, verifiedXfp)
                }
            }
        )
        finish()
    }

    /** Runs the re-add on the restored device; the key type comes from [BackUpSeedPhraseArgs.signer]. */
    private fun openReAddKeyForVerification() {
        navigator.openSignerIntroScreen(
            launcher = signerIntroLauncher,
            activityContext = this,
            walletId = args.walletId,
            groupId = args.groupId,
            // Scopes the key types to the wallet being built; without it the server's
            // inheritance list contributes one card per wallet type and vendors repeat.
            walletType = args.walletType,
            onChainAddSignerParam = OnChainAddSignerParam(
                flags = OnChainAddSignerParam.FLAG_VERIFY_BACKUP_SEED_PHRASE,
                currentSigner = args.signer,
                replaceInfo = OnChainAddSignerParam.ReplaceInfo(
                    replacedXfp = args.replacedXfp.orEmpty(),
                    step = null
                ),
                // Coldcard and air-gap mark the key verified on their own screens, so the method
                // has to travel with the request.
                claimOption = args.claimOption,
            )
        )
        // Stay alive: the device flow reports back through signerIntroLauncher.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEvent()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navHostController = rememberNavController()
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(state.mismatchedSigner) {
                        if (state.mismatchedSigner != null) {
                            // "Try again" leaves this screen on the stack, so a second mismatch
                            // would otherwise pile another copy on top of it.
                            navHostController.navigate(BackUpSeedPhraseVerifyMismatch) {
                                launchSingleTop = true
                            }
                        }
                    }

                    val startDestination = when (args.type) {
                        BackUpSeedPhraseType.INTRO -> BackUpSeedPhraseIntro
                        BackUpSeedPhraseType.SUCCESS -> BackUpSeedPhraseVerifySuccess
                        BackUpSeedPhraseType.INHERITANCE_VERIFIED -> InheritanceSeedPhraseVerified
                    }

                    NavHost(
                        navController = navHostController,
                        startDestination = startDestination
                    ) {
                        backUpSeedPhraseIntroDestination(
                            onContinue = {
                                navHostController.navigate(BackUpSeedPhraseOption)
                            },
                            onMoreClicked = ::handleShowMore
                        )

                        backUpSeedPhraseOptionDestination(
                            walletId = args.walletId,
                            groupId = args.groupId,
                            masterSignerId = args.signer?.fingerPrint.orEmpty(),
                            replacedXfp = args.replacedXfp.orEmpty(),
                            claimOption = args.claimOption,
                            onContinue = {
                                navHostController.navigate(BackUpSeedPhraseVerify)
                            },
                            onSkip = {
                                navigator.returnMembershipScreen()
                            },
                            onMoreClicked = ::handleShowMore
                        )

                        backUpSeedPhraseVerifyDestination(
                            onContinue = ::openReAddKeyForVerification,
                            onMoreClicked = ::handleShowMore
                        )

                        backUpSeedPhraseVerifySuccessDestination(
                            onContinue = {
                                navigator.returnMembershipScreen()
                            },
                            onMoreClicked = ::handleShowMore
                        )

                        inheritanceSeedPhraseVerifiedDestination(
                            signer = args.signer,
                            onContinue = {
                                navigator.returnMembershipScreen()
                            },
                        )

                        backUpSeedPhraseVerifyMismatchDestination(
                            reAddedSigner = state.mismatchedSigner,
                            expectedXfp = args.signer?.fingerPrint.orEmpty(),
                            onTryAgain = {
                                viewModel.onMismatchHandled()
                                openReAddKeyForVerification()
                            },
                            onBackToSteps = {
                                viewModel.onMismatchHandled()
                                navHostController.popBackStack()
                            },
                        )
                    }
                }
            })
    }

    private fun handleShowMore() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_RESTART_WIZARD,
                label = getString(R.string.nc_restart_wizard)

            )
        )
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_EXIT_WIZARD,
                label = getString(R.string.nc_exit_wizard)
            )
        )
        BottomSheetOption.newInstance(options).show(supportFragmentManager, "BottomSheetOption")
    }

    private fun observeEvent() {
        flowObserver(viewModel.event) { event ->
            when (event) {
                is BackUpSeedPhraseEvent.RestartWizardSuccess -> {
                    navigator.openMembershipActivity(
                        activityContext = this,
                        groupStep = MembershipStage.NONE,
                        isPersonalWallet = membershipStepManager.isPersonalWallet(),
                        isClearTop = true,
                        quickWalletParam = null
                    )
                    setResult(Activity.RESULT_OK)
                    finish()
                }
                is BackUpSeedPhraseEvent.Error -> {
                }
                else -> {}
            }
        }
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_RESTART_WIZARD) {
            NCWarningDialog(this).showDialog(
                title = getString(R.string.nc_confirmation),
                message = getString(R.string.nc_confirm_restart_wizard),
                onYesClick = {
                    viewModel.resetWizard(membershipStepManager.localMembershipPlan, args.groupId)
                }
            )
        } else if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
            NCInfoDialog(this).showDialog(
                message = getString(R.string.nc_resume_wizard_desc),
                onYesClick = {
                    finish()
                }
            )
        }
    }

    companion object {
        fun buildIntent(context: Context, args: BackUpSeedPhraseArgs): Intent =
            Intent(context, BackUpSeedPhraseActivity::class.java).apply {
                putExtras(args.buildBundle())
            }

        fun start(context: Context, args: BackUpSeedPhraseArgs) {
            context.startActivity(buildIntent(context, args))
        }
    }
}