package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
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
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackUpSeedPhraseActivity : BaseComposeActivity(), BottomSheetOptionListener {

    private val args: BackUpSeedPhraseArgs by lazy {
        BackUpSeedPhraseArgs.deserializeFrom(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                            }
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
                            }
                        )

                        backUpSeedPhraseVerifySuccessDestination(
                            onContinue = {
                                navigator.returnMembershipScreen()
                            }
                        )
                    }
                }
            })
    }

    private fun handleShowMore() {
        val options = mutableListOf<SheetOption>()
        options.add(
            SheetOption(
                type = SheetOptionType.TYPE_EXIT_WIZARD,
                label = getString(R.string.nc_exit_wizard)
            )
        )
        BottomSheetOption.newInstance(options).show(supportFragmentManager, "BottomSheetOption")
    }

    override fun onOptionClicked(option: SheetOption) {
        if (option.type == SheetOptionType.TYPE_EXIT_WIZARD) {
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