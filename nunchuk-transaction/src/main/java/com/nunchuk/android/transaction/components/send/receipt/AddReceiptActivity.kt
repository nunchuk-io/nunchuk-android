/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.transaction.components.send.receipt

import android.content.Context
import android.content.Intent
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseComposeNfcActivity
import com.nunchuk.android.core.nfc.BaseNfcActivity.Companion.REQUEST_SATSCARD_SWEEP_SLOT
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.InheritanceClaimTxDetailInfo
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.fromSATtoBTC
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.nav.args.AddReceiptArgs
import com.nunchuk.android.nav.args.AddReceiptType
import com.nunchuk.android.nav.args.FeeSettingArgs
import com.nunchuk.android.nav.args.FeeSettingStartDestination
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.batchtransaction.BatchTransactionFragment
import com.nunchuk.android.transaction.components.send.batchtransaction.BatchTransactionFragmentArgs
import com.nunchuk.android.transaction.components.send.batchtransaction.BatchTransactionViewModel
import com.nunchuk.android.transaction.components.send.confirmation.TaprootDraftTransaction
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.returnActiveRoom
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class AddReceiptActivity : BaseComposeNfcActivity() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.fromBundle(intent.extras!!) }

    private val viewModel: AddReceiptViewModel by viewModels()
    private val estimateFeeViewModel: EstimatedFeeViewModel by viewModels()
    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()
    private val transactionConfirmViewModel: TransactionConfirmViewModel by viewModels()
    private val batchTransactionViewModel: BatchTransactionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args)
        transactionConfirmViewModel.init(args.walletId)
        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(
                viewModel.getAddReceiptState().address,
                estimateFeeViewModel.defaultRate
            )
            sweepSatscardViewModel.handleSweepBalance(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
                args.slots.toList(),
                args.sweepType
            )
            nfcViewModel.clearScanInfo()
        }

        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            var draftTx by remember { mutableStateOf<TaprootDraftTransaction?>(null) }
            var dummySigningPaths by remember { mutableStateOf(emptyList<Pair<SigningPath, Amount>>()) }
            var selectingPaths by remember { mutableStateOf<List<SigningPath>>(emptyList()) }
            var timelockCoin by remember { mutableStateOf<TimelockCoin?>(null) }
            val state by viewModel.state.asFlow()
                .collectAsStateWithLifecycle(AddReceiptState())
            Timber.d("CongHai - state.followParents: ${state.subNodeFollowParents}")

            LaunchedEffect(Unit) {
                transactionConfirmViewModel.event.collect {
                    if (it is TransactionConfirmEvent.DraftTaprootTransactionSuccess) {
                        hideLoading()
                        if (it.draftTransaction != null) {
                            draftTx = it.draftTransaction
                            navController.navigate(TaprootFeeSelection)
                        } else {
                            transactionConfirmViewModel.handleConfirmEvent()
                        }
                    } else if (it is TransactionConfirmEvent.ChooseSigningPathsSuccess) {
                        hideLoading()
                        navController.navigate(ChooseSigningPath)
                    } else if (it is TransactionConfirmEvent.ChooseSigningPolicy) {
                        hideLoading()
                        dummySigningPaths = it.result
                        navController.navigate(ChooseSigningPolicy(true)) {
                            popUpTo<ChooseSigningPolicy> {
                                inclusive = true
                            }
                        }
                    } else if (it is TransactionConfirmEvent.ShowTimeLockNotice) {
                        hideLoading()
                        timelockCoin = it.timeLockCoin
                        navController.navigate(TimelockNotice)
                    }
                }
            }

            // miniscript signing policy check
            if (args.type == AddReceiptType.SELECT_PATH) {
                LaunchedEffect(state.wallet) {
                    val wallet = state.wallet
                    if (wallet.id.isNotEmpty()) {
                        if (wallet.addressType.isTaproot() && !state.isValueKeySetDisable) {
                            navController.navigate(ChooseSigningPath) {
                                popUpTo<ChooseSigningPolicy> {
                                    inclusive = true
                                }
                            }
                        } else {
                            transactionConfirmViewModel.checkMiniscriptSigningPolicyTransaction(args.txId.orEmpty())
                        }
                    }
                }
            }

            val startDestination = when (args.type) {
                AddReceiptType.BATCH -> Batch
                AddReceiptType.ADD_RECEIPT -> Main
                AddReceiptType.SELECT_PATH -> ChooseSigningPolicy
            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable<Main> {
                    AndroidFragment(
                        clazz = AddReceiptFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        arguments = intent.extras!!
                    )
                }
                composable<Batch> {
                    AndroidFragment(
                        clazz = BatchTransactionFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        arguments = BatchTransactionFragmentArgs(
                            walletId = args.walletId,
                            isFromSelectCoin = args.inputs.isNotEmpty()
                        ).toBundle()
                    )
                }
                composable<TaprootFeeSelection> {
                    val confirmTransactionUiState by transactionConfirmViewModel.uiState.collectAsStateWithLifecycle()
                    val tx = draftTx
                    LaunchedEffect(tx) {
                        if (tx == null) {
                            navController.popBackStack()
                        }
                    }
                    if (tx != null) {
                        FeeSelectionScreen(
                            isAutoFeeSelectionEnabled = confirmTransactionUiState.feeSelectionSetting.automaticFeeEnabled,
                            draftTx = tx,
                            signers = state.signers,
                            onFeeSettingsClick = {
                                navigator.openFeeSettingsScreen(
                                    this@AddReceiptActivity, args = FeeSettingArgs(
                                        destination = FeeSettingStartDestination.TAPROOT_FEE_SELECTION
                                    )
                                )
                            },
                            onContinue = { keySetIndex ->
                                transactionConfirmViewModel.handleConfirmEvent(keySetIndex = keySetIndex)
                            }
                        )
                    }
                }
                composable<ChooseSigningPath> {
                    val scriptNode = state.scriptNode
                    if (scriptNode != null) {
                        ChooseSigningPathScreen(
                            state = state,
                            onContinue = { isKeyPathSelected ->
                                if (args.type == AddReceiptType.SELECT_PATH) {
                                    if (isKeyPathSelected) {
                                        signingPathSelected(null)
                                    } else {
                                        transactionConfirmViewModel.checkMiniscriptSigningPolicyTransaction(
                                            args.txId.orEmpty()
                                        )
                                    }
                                } else {
                                    if (isKeyPathSelected) {
                                        transactionConfirmViewModel.draftMiniscriptTransaction()
                                    } else {
                                        transactionConfirmViewModel.checkMiniscriptSigningPolicy()
                                    }
                                }
                            },
                        )
                    }
                }
                composable<ChooseSigningPolicy> {
                    val route = it.toRoute<ChooseSigningPolicy>()
                    val scriptNode = state.scriptNode
                    if (scriptNode != null) {
                        SelectScriptPathPolicyScreen(
                            isSelectingPathEnabled = route.isSelectingModeEnabled,
                            scriptNode = scriptNode,
                            signers = state.signers,
                            signingPaths = if (route.isSelectingModeEnabled) dummySigningPaths else dummySigningPaths.filter { (path, _) ->
                                path in selectingPaths
                            },
                            subNodeFollowParents = state.subNodeFollowParents,
                            onContinue = { signingPaths ->
                                if (signingPaths.size == 1) {
                                    val signingPath = signingPaths.first()
                                    if (args.type == AddReceiptType.SELECT_PATH) {
                                        signingPathSelected(signingPath)
                                    } else {
                                        transactionConfirmViewModel.draftMiniscriptTransaction(
                                            signingPath = signingPath
                                        )
                                    }
                                } else if (signingPaths.isNotEmpty()) {
                                    selectingPaths = signingPaths
                                    navController.navigate(ChooseSigningPolicy(false))
                                }
                            }
                        )
                    }
                }
                composable<TimelockNotice> {
                    timelockCoin?.let { timelockCoin ->
                        TimelockNoticeScreen(
                            timelockCoin = timelockCoin,
                            walletId = args.walletId,
                            onContinue = { isSendAll, coins ->
                                if (!isSendAll && args.type == AddReceiptType.BATCH) {
                                    navigator.openInputAmountScreen(
                                        activityContext = this@AddReceiptActivity,
                                        walletId = args.walletId,
                                        availableAmount = coins
                                            .sumOf { it.amount.value }.toDouble().fromSATtoBTC(),
                                        inputs = coins
                                    )
                                } else {
                                    transactionConfirmViewModel.run {
                                        updateInputs(isSendAll, coins)
                                        handleConfirmEvent(
                                            keySetIndex = if (timelockCoin.signingPath != null) 1 else 0,
                                            signingPath = timelockCoin.signingPath
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        observer()
    }

    private fun observer() {
        flowObserver(estimateFeeViewModel.event, collector = ::handleEstimateFeeEvent)
        flowObserver(transactionConfirmViewModel.event, collector = ::handleCreateTransactionEvent)
    }

    private fun signingPathSelected(signingPath: SigningPath?) {
        setResult(RESULT_OK, Intent().apply {
            putExtra(
                GlobalResultKey.SIGNING_PATH,
                signingPath
            )
        })
        finish()
    }

    private fun handleEstimateFeeEvent(event: EstimatedFeeEvent) {
        val state = viewModel.getAddReceiptState()
        if (event is EstimatedFeeEvent.GetFeeRateSuccess) {
            if (args.type == AddReceiptType.BATCH) {
                handleCreateTransaction(
                    txReceipts = batchTransactionViewModel.getTxReceiptList(),
                    subtractFeeFromAmount = batchTransactionViewModel.getSubtractFeeFromAmount(),
                    note = batchTransactionViewModel.getNote(),
                    state = state,
                    event = event
                )
            } else {
                val amount = state.amount
                val address = state.address
                val finalAmount = if (amount.value > 0) amount.pureBTC() else args.outputAmount
                val subtractFeeFromAmount =
                    if (amount.value > 0) false else args.subtractFeeFromAmount
                handleCreateTransaction(
                    txReceipts = listOf(TxReceipt(address, finalAmount)),
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    note = state.privateNote,
                    state = state,
                    event = event
                )
            }
        } else if (event is EstimatedFeeEvent.EstimatedFeeErrorEvent) {
            showEventError(event.message)
        }
    }

    private fun handleCreateTransactionEvent(event: TransactionConfirmEvent) {
        when (event) {
            is TransactionConfirmEvent.CreateTxErrorEvent -> showCreateTransactionError(event.message)
            is TransactionConfirmEvent.CreateTxSuccessEvent -> {
                hideLoading()
                if (transactionConfirmViewModel.isInheritanceClaimingFlow()) {
                    ActivityManager.popUntilRoot()
                    if (event.walletId.isNullOrEmpty()) {
                        navigator.openTransactionDetailsScreen(
                            activityContext = this,
                            walletId = "",
                            txId = event.transaction.txId,
                            transaction = event.transaction,
                            inheritanceClaimTxDetailInfo = InheritanceClaimTxDetailInfo(
                                changePos = event.transaction.changeIndex
                            )
                        )
                    } else {
                        navigator.openTransactionDetailsScreen(
                            activityContext = this,
                            walletId = event.walletId,
                            txId = event.transaction.txId,
                        )
                    }
                } else {
                    navigator.returnToMainScreen(this)
                    openTransactionDetailScreen(
                        event.transaction.txId,
                        args.walletId,
                        sessionHolder.getActiveRoomIdSafe(),
                        isInheritanceClaimingFlow = false
                    )
                }
            }

            is TransactionConfirmEvent.LoadingEvent -> showLoading(
                message = if (event.isClaimInheritance) getString(
                    R.string.nc_withdrawal_in_progress
                ) else null
            )

            is TransactionConfirmEvent.InitRoomTransactionError -> showCreateTransactionError(event.message)
            is TransactionConfirmEvent.InitRoomTransactionSuccess -> returnActiveRoom(event.roomId)
            is TransactionConfirmEvent.UpdateChangeAddress -> {}
            is TransactionConfirmEvent.AssignTagEvent -> {}
            is TransactionConfirmEvent.AssignTagError -> {
                hideLoading()
                NCToastMessage(this).showError(event.message)
            }

            is TransactionConfirmEvent.AssignTagSuccess -> {
                hideLoading()
                NCToastMessage(this).showMessage(getString(R.string.nc_tags_assigned))
                openTransactionDetailScreen(
                    event.txId,
                    args.walletId,
                    sessionHolder.getActiveRoomIdSafe(),
                    transactionConfirmViewModel.isInheritanceClaimingFlow()
                )
            }

            is TransactionConfirmEvent.CustomizeTransaction -> {
                openEstimatedFeeScreen(signingPath = event.signingPath)
            }

            is TransactionConfirmEvent.AutoSelectSigningPath -> signingPathSelected(event.signingPath)

            else -> {}
        }
    }

    fun openEstimatedFeeScreen(
        signingPath: SigningPath? = null
    ) {
        if (args.type == AddReceiptType.BATCH) {
            navigator.openEstimatedFeeScreen(
                activityContext = this,
                walletId = args.walletId,
                availableAmount = args.availableAmount,
                txReceipts = batchTransactionViewModel.getTxReceiptList(),
                privateNote = batchTransactionViewModel.getNote(),
                subtractFeeFromAmount = batchTransactionViewModel.getSubtractFeeFromAmount(),
                sweepType = args.sweepType,
                slots = args.slots,
                inputs = args.inputs,
                claimInheritanceTxParam = args.claimInheritanceTxParam,
                signingPath = signingPath
            )
        } else {
            val state = viewModel.getAddReceiptState()
            val amount = state.amount
            val address = state.address
            val privateNote = state.privateNote
            val finalAmount = if (amount.value > 0) amount.pureBTC() else args.outputAmount
            val subtractFeeFromAmount = if (amount.value > 0) false else args.subtractFeeFromAmount
            navigator.openEstimatedFeeScreen(
                activityContext = this,
                walletId = args.walletId,
                availableAmount = args.availableAmount,
                txReceipts = listOf(TxReceipt(address, finalAmount)),
                privateNote = privateNote,
                subtractFeeFromAmount = subtractFeeFromAmount,
                sweepType = args.sweepType,
                slots = args.slots,
                inputs = args.inputs,
                claimInheritanceTxParam = args.claimInheritanceTxParam,
                signingPath = signingPath
            )
        }
    }

    private fun handleCreateTransaction(
        txReceipts: List<TxReceipt>,
        subtractFeeFromAmount: Boolean,
        note: String,
        state: AddReceiptState,
        event: EstimatedFeeEvent.GetFeeRateSuccess
    ) {
        if (args.slots.isNotEmpty()) {
            startNfcFlow(REQUEST_SATSCARD_SWEEP_SLOT)
        } else {
            val manualFeeRate =
                if (transactionConfirmViewModel.isInheritanceClaimingFlow()) event.estimateFeeRates.priorityRate else event.estimateFeeRates.defaultRate
            transactionConfirmViewModel.init(
                walletId = args.walletId,
                txReceipts = txReceipts,
                subtractFeeFromAmount = subtractFeeFromAmount,
                privateNote = note,
                manualFeeRate = manualFeeRate,
                slots = args.slots,
                inputs = args.inputs,
                claimInheritanceTxParam = args.claimInheritanceTxParam,
                antiFeeSniping = viewModel.getAddReceiptState().antiFeeSniping,
            )
            if (state.scriptNode != null) {
                if (state.addressType.isTaproot() && !state.isValueKeySetDisable) {
                    transactionConfirmViewModel.checkMiniscriptSigningPaths()
                } else {
                    transactionConfirmViewModel.checkMiniscriptSigningPolicy()
                }
            } else if (state.addressType.isTaproot() && !state.isValueKeySetDisable) {
                transactionConfirmViewModel.checkShowTaprootDraftTransaction()
            } else {
                transactionConfirmViewModel.handleConfirmEvent(true)
            }
        }
    }

    private fun showEventError(message: String) {
        NCToastMessage(this).showError(message)
    }

    val availableAmount: Double
        get() = args.availableAmount

    companion object {

        fun selectMiniscriptSigningPath(
            activityContext: Context,
            launcher: ActivityResultLauncher<Intent>,
            walletId: String,
            txId: String?,
        ) {
            launcher.launch(
                Intent(activityContext, AddReceiptActivity::class.java).apply {
                    putExtras(
                        AddReceiptArgs(
                            walletId = walletId,
                            type = AddReceiptType.SELECT_PATH,
                            txId = txId
                        ).buildBundle()
                    )
                }
            )
        }

        fun start(
            activityContext: Context,
            walletId: String,
            outputAmount: Double,
            availableAmount: Double,
            address: String = "",
            privateNote: String = "",
            subtractFeeFromAmount: Boolean = false,
            slots: List<SatsCardSlot> = emptyList(),
            sweepType: SweepType = SweepType.NONE,
            inputs: List<UnspentOutput> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            receiptType: AddReceiptType = AddReceiptType.ADD_RECEIPT,
        ) {
            val intent = Intent(activityContext, AddReceiptActivity::class.java).apply {
                putExtras(
                    AddReceiptArgs(
                        walletId = walletId,
                        outputAmount = outputAmount,
                        availableAmount = availableAmount,
                        subtractFeeFromAmount = subtractFeeFromAmount,
                        slots = slots,
                        address = address,
                        privateNote = privateNote,
                        sweepType = sweepType,
                        inputs = inputs,
                        claimInheritanceTxParam = claimInheritanceTxParam,
                        type = receiptType
                    ).buildBundle()
                )
            }
            activityContext.startActivity(intent)
        }
    }
}