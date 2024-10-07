package com.nunchuk.android.main.membership.byzantine.payment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.nunchuk.android.core.base.BaseComposeActivity
import com.nunchuk.android.core.wallet.AssistedWalletBottomSheet
import com.nunchuk.android.core.wallet.WalletBottomSheetResult
import com.nunchuk.android.core.wallet.WalletComposeBottomSheet
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.payment.address.wallet.addPaymentWalletAddress
import com.nunchuk.android.main.membership.byzantine.payment.address.wallet.navigateToPaymentWalletAddress
import com.nunchuk.android.main.membership.byzantine.payment.address.whitelist.addWhitelistAddress
import com.nunchuk.android.main.membership.byzantine.payment.address.whitelist.navigateToWhitelistAddress
import com.nunchuk.android.main.membership.byzantine.payment.amount.addPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.amount.navigateToPaymentAmount
import com.nunchuk.android.main.membership.byzantine.payment.cosign.addPaymentCosignScreen
import com.nunchuk.android.main.membership.byzantine.payment.cosign.navigateToPaymentCosign
import com.nunchuk.android.main.membership.byzantine.payment.detail.addRecurringPaymentDetail
import com.nunchuk.android.main.membership.byzantine.payment.detail.navigateToRecurringPaymentDetail
import com.nunchuk.android.main.membership.byzantine.payment.feerate.addPaymentFeeRateScreen
import com.nunchuk.android.main.membership.byzantine.payment.feerate.navigateToPaymentFeeRate
import com.nunchuk.android.main.membership.byzantine.payment.frequent.addPaymentFrequency
import com.nunchuk.android.main.membership.byzantine.payment.frequent.navigateToPaymentFrequency
import com.nunchuk.android.main.membership.byzantine.payment.list.recurringPaymentsList
import com.nunchuk.android.main.membership.byzantine.payment.name.addPaymentName
import com.nunchuk.android.main.membership.byzantine.payment.name.navigateToPaymentName
import com.nunchuk.android.main.membership.byzantine.payment.note.addPaymentNoteScreen
import com.nunchuk.android.main.membership.byzantine.payment.note.navigateToPaymentNote
import com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage.addPaymentPercentageCalculation
import com.nunchuk.android.main.membership.byzantine.payment.paymentpercentage.navigateToPaymentPercentageCalculation
import com.nunchuk.android.main.membership.byzantine.payment.qr.addQRDetail
import com.nunchuk.android.main.membership.byzantine.payment.qr.navigateToQRDetail
import com.nunchuk.android.main.membership.byzantine.payment.selectmethod.addPaymentSelectAddressType
import com.nunchuk.android.main.membership.byzantine.payment.selectmethod.navigateToPaymentSelectAddressType
import com.nunchuk.android.main.membership.byzantine.payment.summary.addPaymentSummary
import com.nunchuk.android.main.membership.byzantine.payment.summary.navigateToPaymentSummary
import com.nunchuk.android.model.VerificationType
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.DummyTransactionPayload
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.utils.parcelable
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecurringPaymentActivity : BaseComposeActivity() {

    private val viewModel: RecurringPaymentViewModel by viewModels()

    private val groupId: String by lazy {
        intent.getStringExtra(GROUP_ID).orEmpty()
    }

    private val walletId: String by lazy {
        intent.getStringExtra(WALLET_ID).orEmpty()
    }

    private val signLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                finish()
            }
        }

    private val scanWalletLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                result.data?.parcelable<Wallet>(GlobalResultKey.WALLET)?.let { wallet ->
                    viewModel.getBsms(wallet)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.setFragmentResultListener(
            WalletComposeBottomSheet.TAG, this
        ) { _, bundle ->
            val result = bundle.parcelable<WalletBottomSheetResult>(WalletComposeBottomSheet.RESULT) ?: return@setFragmentResultListener
            val selectedWalletId = result.walletId ?: return@setFragmentResultListener
            viewModel.getWalletDetail(selectedWalletId)
        }
        setContentView(
            ComposeView(this).apply {
                setContent {
                    val navController = rememberNavController()
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    NavHost(
                        navController = navController,
                        startDestination = "recurring_payment/${groupId}/${walletId}"
                    ) {
                        recurringPaymentsList(
                            onOpenAddRecurringPayment = {
                                viewModel.init()
                                navController.navigateToPaymentName()
                            },
                            groupId = groupId,
                            walletId = walletId,
                            myRole = viewModel.myRole,
                            onOpenRecurringPaymentDetail = { recurringPaymentId ->
                                navController.navigateToRecurringPaymentDetail(
                                    groupId = groupId,
                                    walletId = walletId,
                                    recurringPaymentId = recurringPaymentId,
                                )
                            },
                        )
                        addPaymentName(
                            recurringPaymentViewModel = viewModel,
                            openPaymentAmountScreen = {
                                navController.navigateToPaymentAmount()
                            },
                        )
                        addPaymentAmount(
                            recurringPaymentViewModel = viewModel,
                            openCalculateScreen = {
                                navController.navigateToPaymentPercentageCalculation()
                            },
                            openSelectAddressTypeScreen = {
                                navController.navigateToPaymentSelectAddressType()
                            },
                        )
                        addPaymentSelectAddressType(
                            recurringPaymentViewModel = viewModel,
                            openWhiteListAddressScreen = {
                                navController.navigateToWhitelistAddress()
                            },
                            openScanQRCodeScreen = {
                                navigator.openParseWalletQRCodeScreen(
                                    launcher = scanWalletLauncher,
                                    activityContext = this@RecurringPaymentActivity
                                )
                            },
                            openBsmsScreen = {
                                navController.navigateToPaymentWalletAddress()
                            },
                            openScanMk4 = {
                                navigator.startSetupMk4ForResult(
                                    launcher = scanWalletLauncher,
                                    activity = this@RecurringPaymentActivity,
                                    fromMembershipFlow = false,
                                    action = it,
                                )
                            },
                            openSellectWallet = {
                                WalletComposeBottomSheet.show(
                                    supportFragmentManager,
                                    assistedWalletIds = state.otherwallets.map { wallet -> wallet.id },
                                    configArgs = WalletComposeBottomSheet.ConfigArgs(
                                        title = context.getString(R.string.nc_select_a_wallet),
                                    ),
                                )
                            }
                        )
                        addPaymentPercentageCalculation(
                            recurringPaymentViewModel = viewModel,
                            openSelectAddressTypeScreen = {
                                navController.navigateToPaymentSelectAddressType()
                            },
                        )
                        addWhitelistAddress(
                            paymentViewModel = viewModel,
                            openPaymentFrequencyScreen = {
                                navController.navigateToPaymentFrequency()
                            },
                        )
                        addPaymentFrequency(
                            recurringPaymentViewModel = viewModel,
                            openPaymentFeeRateScreen = {
                                navController.navigateToPaymentFeeRate()
                            },
                        )
                        addPaymentCosignScreen(
                            recurringPaymentViewModel = viewModel,
                            openPaymentNote = {
                                navController.navigateToPaymentNote()
                            },
                        )
                        addPaymentNoteScreen(
                            recurringPaymentViewModel = viewModel,
                            openSummaryScreen = {
                                navController.navigateToPaymentSummary()
                            },
                        )
                        addPaymentFeeRateScreen(
                            recurringPaymentViewModel = viewModel,
                            openPaymentCosignScreen = {
                                if (state.hasServerKey) {
                                    navController.navigateToPaymentCosign()
                                } else {
                                    navController.navigateToPaymentNote()
                                }
                            },
                        )
                        addQRDetail()
                        addPaymentSummary(
                            recurringPaymentViewModel = viewModel,
                            openDummyTransactionScreen = ::openWalletAuthentication,
                            openQRDetailScreen = { navController.navigateToQRDetail(it) }
                        )
                        addRecurringPaymentDetail(
                            onOpenDummyTransaction = ::openWalletAuthentication,
                            openQRDetailScreen = { navController.navigateToQRDetail(it) }

                        )
                        addPaymentWalletAddress(
                            recurringPaymentViewModel = viewModel,
                            openPaymentFrequencyScreen = {
                                navController.navigateToPaymentFrequency()
                            },
                            onOpenQrDetailScreen = { navController.navigateToQRDetail(it) }
                        )
                    }
                }
            }
        )
    }

    private fun openWalletAuthentication(payload: DummyTransactionPayload) {
        navigator.openWalletAuthentication(
            walletId = payload.walletId,
            userData = "",
            requiredSignatures = payload.requiredSignatures,
            type = VerificationType.SIGN_DUMMY_TX,
            launcher = signLauncher,
            activityContext = this@RecurringPaymentActivity,
            groupId = groupId,
            dummyTransactionId = payload.dummyTransactionId,
        )
    }

    companion object {
        internal const val GROUP_ID = "group_id"
        internal const val WALLET_ID = "wallet_id"
        internal const val ROLE = "role"

        fun navigate(
            activity: Context,
            groupId: String,
            walletId: String?,
            role: AssistedWalletRole,
        ) {
            val intent = Intent(activity, RecurringPaymentActivity::class.java).apply {
                putExtra(GROUP_ID, groupId)
                putExtra(WALLET_ID, walletId)
                putExtra(ROLE, role)
            }
            activity.startActivity(intent)
        }
    }
}