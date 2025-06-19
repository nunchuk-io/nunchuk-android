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
import com.nunchuk.android.app.miniscript.custom.MiniscriptCustomTemplate
import com.nunchuk.android.app.miniscript.custom.miniscriptCustomTemplateDestination
import com.nunchuk.android.app.miniscript.intro.MiniscriptIntro
import com.nunchuk.android.app.miniscript.intro.miniscriptIntroDestination
import com.nunchuk.android.app.miniscript.reviewwallet.MiniscriptReviewWallet
import com.nunchuk.android.app.miniscript.reviewwallet.miniscriptReviewWalletDestination
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.nav.args.BackUpWalletArgs
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.widget.NCWarningDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import timber.log.Timber

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
                            handleCacheXpub()
                            sharedWalletViewModel.resetRequestCacheTapSignerXpub()
                        }
                    }

                    NavHost(
                        navController = navHostController,
                        startDestination = MiniscriptIntro
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
                            addressType = args.addressType
                        ) { template ->
                            navHostController.navigate(
                                route = MiniscriptCustomTemplate(
                                    template = template,
                                    addressType = args.addressType,
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

                        miniscriptCustomTemplateDestination { template, addressType ->
                            navHostController.navigate(
                                route = MiniscriptConfigureWallet(
                                    template = template,
                                    addressType = addressType ?: args.addressType,
                                    walletName = args.walletName,
                                )
                            )
                        }

                        miniscriptReviewWalletDestination(
                            viewModel = sharedWalletViewModel
                        ) { wallet ->
                            Timber.tag("miniscript-feature")
                                .d("Navigation callback received wallet: $wallet")
                            navigator.openBackupWalletScreen(
                                activityContext = this@MiniscriptActivity,
                                args = BackUpWalletArgs(wallet = wallet)
                            )
                            Timber.tag("miniscript-feature").d("Opening backup wallet screen")
                            finish()
                        }
                    }
                }
            })
    }

    override fun onInputDone(masterSignerId: String, newInput: String) {
        sharedWalletViewModel.updateBip32Path(masterSignerId, newInput)
    }

    private fun handleCacheXpub() {
        NCWarningDialog(this).showDialog(
            title = getString(com.nunchuk.android.core.R.string.nc_text_info),
            message = getString(com.nunchuk.android.core.R.string.nc_new_xpub_need),
            onYesClick = {
                startNfcFlow(REQUEST_NFC_TOPUP_XPUBS)
            }
        )
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