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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.R
import com.nunchuk.android.app.miniscript.configurewallet.MiniscriptConfigureWallet
import com.nunchuk.android.app.miniscript.configurewallet.miniscriptConfigureWalletDestination
import com.nunchuk.android.app.miniscript.contractpolicy.MiniscriptConfigTemplate
import com.nunchuk.android.app.miniscript.contractpolicy.miniscriptConfigTemplateDestination
import com.nunchuk.android.app.miniscript.contractpolicy.miniscriptSelectTimeZoneDestination
import com.nunchuk.android.app.miniscript.custom.MiniscriptCustomTemplate
import com.nunchuk.android.app.miniscript.custom.miniscriptCustomTemplateDestination
import com.nunchuk.android.app.miniscript.intro.MiniscriptIntro
import com.nunchuk.android.app.miniscript.intro.miniscriptIntroDestination
import com.nunchuk.android.app.miniscript.replacekey.MiniscriptReplaceIntro
import com.nunchuk.android.app.miniscript.replacekey.MiniscriptReplaceKeyRoute
import com.nunchuk.android.app.miniscript.replacekey.miniscriptReplaceKeyDestination
import com.nunchuk.android.app.miniscript.replacekey.miniscriptReplaceKeyIntroDestination
import com.nunchuk.android.app.miniscript.reviewwallet.MiniscriptReviewWallet
import com.nunchuk.android.app.miniscript.reviewwallet.miniscriptReviewWalletDestination
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.miniscript.MultisignType
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.push.PushEvent
import com.nunchuk.android.core.util.RollOverWalletFlow
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.signer.SignerIntroFlow
import com.nunchuk.android.core.signer.SignerIntroRequest
import com.nunchuk.android.core.util.navigateToSelectWallet
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.membership.wallet.createWalletSuccessScreen
import com.nunchuk.android.main.membership.wallet.navigateCreateWalletSuccessScreen
import com.nunchuk.android.nav.args.MiniscriptArgs
import com.nunchuk.android.usecase.GetWalletUseCase
import com.nunchuk.android.wallet.InputBipPathBottomSheet
import com.nunchuk.android.wallet.InputBipPathBottomSheetListener
import com.nunchuk.android.widget.NCInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MiniscriptActivity : BaseComposeNfcActivity(), InputBipPathBottomSheetListener {

    @Inject
    lateinit var getWalletUseCase: GetWalletUseCase

    private val args: MiniscriptArgs by lazy {
        MiniscriptArgs.deserializeFrom(
            intent.extras ?: Bundle()
        )
    }

    private val sharedWalletViewModel: MiniscriptSharedWalletViewModel by viewModels()

    private var newWalletId: String? = null

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
                            startNfcFlow(REQUEST_NFC_TOPUP_XPUBS, getString(R.string.nc_miniscript_rescan_tapsigner_new_xpub))
                            sharedWalletViewModel.resetRequestCacheTapSignerXpub()
                        }
                    }

                    val startDestination = when {
                        args.replaceWalletId.isNotEmpty() -> MiniscriptReplaceIntro(
                            isReplaceKeyWithTimelock = args.isReplaceKeyWithTimelock,
                            walletId = args.replaceWalletId
                        )

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
                                    request = SignerIntroRequest(
                                        flow = SignerIntroFlow.AddWalletKey,
                                        groupId = "-1",
                                        supportedSigners = supportedSigners,
                                        // A slot that reuses one key across policies needs an xpub per
                                        // policy. Ledger and BitBox read them all in one session so the
                                        // user pairs the device once; the other flows add one key.
                                        accountCount = sharedWalletViewModel.getRequiredAccountCount(),
                                    ),
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
                            ActivityManager.popUntilRoot()
                        }

                        miniscriptSelectTimeZoneDestination(navController = navHostController)

                        miniscriptReplaceKeyIntroDestination(
                            onContinueClicked = { walletId ->
                                navHostController.navigate(
                                    route = MiniscriptReplaceKeyRoute(
                                        walletId = walletId
                                    )
                                )
                            }
                        )

                        miniscriptReplaceKeyDestination(
                            onCreateWalletSuccess = { wallet ->
                                newWalletId = wallet.id
                                navHostController.navigateCreateWalletSuccessScreen(
                                    walletId = wallet.id,
                                    replacedWalletId = args.replaceWalletId
                                )
                            }
                        )

                        createWalletSuccessScreen(
                            onBackPress = {
                                navigateToSelectWallet(
                                    navigator = navigator,
                                    quickWalletParam = null
                                ) {
                                    returnToMainScreen()
                                }
                            },
                            onContinueClicked = {
                                if (args.replaceWalletId.isNotEmpty() && newWalletId != null) {
                                    checkWalletBalance(
                                        replacedWalletId = args.replaceWalletId,
                                        newWalletId = newWalletId!!
                                    )
                                } else {
                                    navigateToSelectWallet(
                                        navigator = navigator,
                                        quickWalletParam = null
                                    ) {
                                        returnToMainScreen()
                                    }
                                }
                            }
                        )
                    }
                }
            })
    }

    private fun returnToMainScreen() = lifecycleScope.launch {
        navigator.returnToMainScreen(this@MiniscriptActivity)
        pushEventManager.push(PushEvent.CloseWalletDetail)
    }

    private fun checkWalletBalance(replacedWalletId: String, newWalletId: String) {
        lifecycleScope.launch {
            getWalletUseCase.execute(replacedWalletId).collect { replacedWallet ->
                if (replacedWallet.wallet.balance.pureBTC() == 0.0) {
                    returnToMainScreen()
                } else {
                    NCInfoDialog(this@MiniscriptActivity)
                        .showDialog(
                            title = getString(com.nunchuk.android.main.R.string.nc_confirmation),
                            message = getString(com.nunchuk.android.main.R.string.nc_transfer_fund_desc),
                            btnYes = getString(com.nunchuk.android.main.R.string.nc_yes_do_it_now),
                            btnInfo = getString(com.nunchuk.android.main.R.string.nc_i_ll_do_it_later),
                            onYesClick = {
                                navigator.openRollOverWalletScreen(
                                    activityContext = this@MiniscriptActivity,
                                    oldWalletId = replacedWalletId,
                                    newWalletId = newWalletId,
                                    startScreen = RollOverWalletFlow.REFUND,
                                    source = RollOverWalletSource.REPLACE_KEY
                                )
                            },
                            onInfoClick = {
                                returnToMainScreen()
                                navigator.openWalletDetailsScreen(
                                    activityContext = this@MiniscriptActivity,
                                    walletId = newWalletId
                                )
                            }
                        )
                }
            }
        }
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