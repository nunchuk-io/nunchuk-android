package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.signer.OnChainAddSignerParam
import com.nunchuk.android.core.util.BackUpSeedPhraseType
import com.nunchuk.android.nav.args.BackUpSeedPhraseArgs
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BackUpSeedPhraseActivity : BaseComposeActivity() {

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
                            groupId = args.groupId,
                            masterSignerId = args.signer?.fingerPrint.orEmpty(),
                            onContinue = {
                                navHostController.navigate(BackUpSeedPhraseVerify)
                            },
                            onSkip = {
                                navigator.returnMembershipScreen()
                            }
                        )

                        backUpSeedPhraseVerifyDestination(
                            onContinue = {
                                navigator.openSignerIntroScreen(
                                    activityContext = this@BackUpSeedPhraseActivity,
                                    walletId = args.walletId,
                                    groupId = args.groupId,
                                    onChainAddSignerParam = OnChainAddSignerParam(
                                        flags = OnChainAddSignerParam.FLAG_VERIFY_BACKUP_SEED_PHRASE,
                                        currentSigner = args.signer
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

    companion object {
        fun start(context: Context, args: BackUpSeedPhraseArgs) {
            context.startActivity(Intent(context, BackUpSeedPhraseActivity::class.java).apply {
                putExtras(args.buildBundle())
            })
        }
    }
}