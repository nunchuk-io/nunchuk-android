package com.nunchuk.android.app.miniscript

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.app.miniscript.configurewallet.MiniscriptConfigureWallet
import com.nunchuk.android.app.miniscript.configurewallet.miniscriptConfigureWalletDestination
import com.nunchuk.android.app.miniscript.contractpolicy.MiniscriptConfigTemplate
import com.nunchuk.android.app.miniscript.contractpolicy.miniscriptConfigTemplateDestination
import com.nunchuk.android.app.miniscript.contractpolicy.miniscriptSelectTimeZoneDestination
import com.nunchuk.android.app.miniscript.custom.MiniscriptCustomTemplate
import com.nunchuk.android.app.miniscript.custom.miniscriptCustomTemplateDestination
import com.nunchuk.android.app.miniscript.intro.MiniscriptIntro
import com.nunchuk.android.app.miniscript.intro.miniscriptIntroDestination
import com.nunchuk.android.app.miniscript.reviewwallet.MiniscriptReviewWallet
import com.nunchuk.android.app.miniscript.reviewwallet.miniscriptReviewWalletDestination
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class MiniscriptActivity : BaseComposeNfcActivity(), InputBipPathBottomSheetListener {

    private val args: MiniscriptArgs by lazy {
        MiniscriptArgs.deserializeFrom(
            intent.extras ?: Bundle()
        )
    }

    private val sharedWalletViewModel: MiniscriptSharedWalletViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle NFC flow for TapSigner xpub caching
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_NFC_TOPUP_XPUBS }) {
            sharedWalletViewModel.cacheTapSignerXpub(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
            )
            nfcViewModel.clearScanInfo()
        }

        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navHostController = rememberNavController()
                    val uiState by sharedWalletViewModel.uiState.collectAsStateWithLifecycle()

                    LaunchedEffect(uiState.requestCacheTapSignerXpubEvent) {
                        if (uiState.requestCacheTapSignerXpubEvent) {
                            startNfcFlow(REQUEST_NFC_TOPUP_XPUBS, "Please rescan your TAPSIGNER to get a new XPUB")
                            sharedWalletViewModel.resetRequestCacheTapSignerXpub()
                        }
                    }

                    val startDestination = when {
                        args.fromAddWallet && args.multisignType == MultisignType.CUSTOM -> MiniscriptCustomTemplate(
                            template = args.template,
                            addressType = args.addressType
                        )

                        args.fromAddWallet && args.multisignType == MultisignType.IMPORT -> MiniscriptCustomTemplate(
                            template = args.template,
                            addressType = args.addressType
                        )

                        args.fromAddWallet -> MiniscriptConfigTemplate(
                            multisignType = args.multisignType.ordinal
                        )

                        else -> MiniscriptIntro
                    }

                    NavHost(
                        navController = navHostController,
                        startDestination = startDestination
                    ) {
                        miniscriptIntroDestination(
                            addressType = args.addressType,
                            onSelect = {
                                if (it == MultisignType.CUSTOM) {
                                    navHostController.navigate(
                                        route = MiniscriptCustomTemplate(
                                            template = "",
                                            addressType = args.addressType,
                                        )
                                    )
                                } else {
                                    navHostController.navigate(
                                        route = MiniscriptConfigTemplate(
                                            multisignType = it.ordinal
                                        )
                                    )
                                }
                            }, onNavigateToCustomTemplate = {
                                navHostController.navigate(
                                    route = MiniscriptCustomTemplate(
                                        template = it,
                                        addressType = args.addressType,
                                    )
                                )
                            })

                        miniscriptConfigTemplateDestination(
                            addressType = args.addressType,
                        ) { template, reuseSigner ->
                            navHostController.navigate(
                                route = MiniscriptCustomTemplate(
                                    template = template,
                                    addressType = args.addressType,
                                    reuseSigner = reuseSigner
                                )
                            )
                        }

                        miniscriptConfigureWalletDestination(
                            viewModel = sharedWalletViewModel,
                            onAddNewKey = { supportedSigners ->
                                navigator.openSignerIntroScreen(
                                    activityContext = this@MiniscriptActivity,
                                    groupId = "-1",
                                    supportedSigners = supportedSigners
                                )
                            },
                            onContinue = {
                                navHostController.navigate(
                                    route = MiniscriptReviewWallet
                                )
                            },
                            onOpenChangeBip32Path = { signer ->
                                InputBipPathBottomSheet.show(
                                    supportFragmentManager,
                                    signer.id,
                                    signer.derivationPath
                                )
                            }
                        )

                        miniscriptCustomTemplateDestination(
                            fromAddWallet = args.fromAddWallet,
                            onNext = { template, addressType, reuseSigner ->
                                navHostController.navigate(
                                    route = MiniscriptConfigureWallet(
                                        template = template,
                                        addressType = addressType ?: args.addressType,
                                        walletName = args.walletName,
                                        reuseSigner = reuseSigner
                                    )
                                )
                            },
                            onSaveAndBack = { template, addressType, reuseSigner ->
                                if (args.fromAddWallet) {
                                    val resultIntent = Intent().apply {
                                        putExtra("miniscript_template", template)
                                        addressType?.let { putExtra("address_type", it.name) }
                                        putExtra("reuse_signer", reuseSigner)
                                    }
                                    setResult(RESULT_OK, resultIntent)
                                }
                                finish()
                            }
                        )

                        miniscriptReviewWalletDestination(
                            viewModel = sharedWalletViewModel
                        ) { wallet ->
                            navigator.openWalletDetailsScreen(
                                activityContext = this@MiniscriptActivity,
                                walletId = wallet.id
                            )
                            finish()
                        }

                        miniscriptSelectTimeZoneDestination(navController = navHostController)
                    }
                }
            })
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        sharedWalletViewModel.updateBip32Path(masterSignerId, newInput)
    }

    companion object {
        private const val REQUEST_NFC_TOPUP_XPUBS = 2001

        fun start(context: Context, args: MiniscriptArgs) {
            context.startActivity(Intent(context, MiniscriptActivity::class.java).apply {
                putExtras(args.buildBundle())
            })
        }
    }
}