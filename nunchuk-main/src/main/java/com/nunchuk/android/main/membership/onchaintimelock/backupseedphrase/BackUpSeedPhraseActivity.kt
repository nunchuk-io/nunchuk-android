package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
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
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.model.MembershipStage
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.share.membership.MembershipStepManager
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEvent()

        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navHostController = rememberNavController()

                    val startDestination = when (args.type) {
                        BackUpSeedPhraseType.INTRO -> BackUpSeedPhraseIntro
                        BackUpSeedPhraseType.SUCCESS -> BackUpSeedPhraseVerifySuccess
                        else -> {}
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
                            onContinue = {
                                navHostController.navigate(BackUpSeedPhraseVerify)
                            },
                            onSkip = {
                                navigator.returnMembershipScreen()
                            },
                            onMoreClicked = ::handleShowMore
                        )

                        backUpSeedPhraseVerifyDestination(
                            onContinue = {
                                navigator.openSignerIntroScreen(
                                    activityContext = this@BackUpSeedPhraseActivity,
                                    walletId = args.walletId,
                                    groupId = args.groupId,
                                    onChainAddSignerParam = OnChainAddSignerParam(
                                        flags = OnChainAddSignerParam.FLAG_VERIFY_BACKUP_SEED_PHRASE,
                                        currentSigner = args.signer,
                                        replaceInfo = OnChainAddSignerParam.ReplaceInfo(
                                            replacedXfp = args.replacedXfp.orEmpty(),
                                            step = null
                                        )
                                    )
                                )
                                finish()
                            },
                            onMoreClicked = ::handleShowMore
                        )

                        backUpSeedPhraseVerifySuccessDestination(
                            onContinue = {
                                navigator.returnMembershipScreen()
                            },
                            onMoreClicked = ::handleShowMore
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
        fun start(context: Context, args: BackUpSeedPhraseArgs) {
            context.startActivity(Intent(context, BackUpSeedPhraseActivity::class.java).apply {
                putExtras(args.buildBundle())
            })
        }
    }
}