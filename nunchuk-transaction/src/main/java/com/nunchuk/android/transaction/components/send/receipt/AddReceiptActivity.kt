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
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.util.InheritanceClaimTxDetailInfo
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.defaultRate
import com.nunchuk.android.nav.args.FeeSettingArgs
import com.nunchuk.android.nav.args.FeeSettingStartDestination
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.send.confirmation.TaprootDraftTransaction
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmViewModel
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeEvent
import com.nunchuk.android.transaction.components.send.fee.EstimatedFeeViewModel
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.returnActiveRoom
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.transaction.databinding.ActivityTransactionAddReceiptBinding
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import javax.inject.Inject


@AndroidEntryPoint
class AddReceiptActivity : BaseNfcActivity<ActivityTransactionAddReceiptBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: AddReceiptArgs by lazy { AddReceiptArgs.deserializeFrom(intent) }

    private val viewModel: AddReceiptViewModel by viewModels()
    private val estimateFeeViewModel: EstimatedFeeViewModel by viewModels()
    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()
    private val transactionConfirmViewModel: TransactionConfirmViewModel by viewModels()

    override fun initializeBinding() = ActivityTransactionAddReceiptBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            LaunchedEffect(Unit) {
                transactionConfirmViewModel.event.collect {
                    if (it is TransactionConfirmEvent.DraftTaprootTransactionSuccess) {
                        if (it.draftTransaction != null) {
                            draftTx = it.draftTransaction
                            navController.navigate(ReceiptNavigation.TaprootFeeSelection)
                        } else {
                            transactionConfirmViewModel.handleConfirmEvent()
                        }
                    } else if (it is TransactionConfirmEvent.ChooseSigningPathsSuccess) {
                        navController.navigate(ReceiptNavigation.ChooseSigningPath)
                    } else if (it is TransactionConfirmEvent.ChooseSigningPolicy) {
                        dummySigningPaths = it.result
                        navController.navigate(ReceiptNavigation.ChooseSigningPolicy)
                    }
                }
            }

            NavHost(
                navController = navController,
                startDestination = ReceiptNavigation.Main
            ) {
                composable<ReceiptNavigation.Main> {
                    AndroidFragment(
                        AddReceiptFragment::class.java,
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        arguments = intent.extras!!
                    )
                }
                composable<ReceiptNavigation.TaprootFeeSelection> {
                    val state by viewModel.state.asFlow()
                        .collectAsStateWithLifecycle(AddReceiptState())
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
                composable<ReceiptNavigation.ChooseSigningPath> {
                    val state by viewModel.state.asFlow()
                        .collectAsStateWithLifecycle(AddReceiptState())
                    val scriptNode = state.scriptNode
                    if (scriptNode != null) {
                        ChooseSigningPathScreen(
                            wallet = state.wallet,
                            signers = state.signers,
                            scriptNode = scriptNode,
                            onContinue = { isKeyPathSelected ->
                                if (isKeyPathSelected) {
                                    transactionConfirmViewModel.handleConfirmEvent(true)
                                } else {
                                    transactionConfirmViewModel.checkMiniscriptSigningPolicy()
                                }
                            },
                        )
                    }
                }
                composable<ReceiptNavigation.ChooseSigningPolicy> {
                    val state by viewModel.state.asFlow().collectAsStateWithLifecycle(AddReceiptState())
                    val scriptNode = state.scriptNode
                    if (scriptNode != null) {
                        SelectScriptPathPolicyScreen(
                            scriptNode = scriptNode,
                            signers = state.signers,
                            signingPaths = dummySigningPaths,
                            onContinue = { /* handle continue */ }
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


    private fun handleEstimateFeeEvent(event: EstimatedFeeEvent) {
        val state = viewModel.getAddReceiptState()
        val amount = state.amount
        val address = state.address
        if (event is EstimatedFeeEvent.GetFeeRateSuccess) {
            handleCreateTransaction(amount, address, state, event)
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
                    navigator.openTransactionDetailsScreen(
                        activityContext = this,
                        walletId = "",
                        txId = event.transaction.txId,
                        initEventId = "",
                        roomId = "",
                        transaction = event.transaction,
                        inheritanceClaimTxDetailInfo = InheritanceClaimTxDetailInfo(
                            changePos = event.transaction.changeIndex
                        )
                    )
                } else {
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

            else -> {}
        }
    }


    private fun handleCreateTransaction(
        amount: Amount,
        address: String,
        state: AddReceiptState,
        event: EstimatedFeeEvent.GetFeeRateSuccess
    ) {
        if (args.slots.isNotEmpty()) {
            startNfcFlow(REQUEST_SATSCARD_SWEEP_SLOT)
        } else {
            val finalAmount = if (amount.value > 0) amount.pureBTC() else args.outputAmount
            val subtractFeeFromAmount = if (amount.value > 0) false else args.subtractFeeFromAmount
            val manualFeeRate =
                if (transactionConfirmViewModel.isInheritanceClaimingFlow()) event.estimateFeeRates.priorityRate else event.estimateFeeRates.defaultRate
            transactionConfirmViewModel.init(
                walletId = args.walletId,
                txReceipts = listOf(TxReceipt(address, finalAmount)),
                privateNote = state.privateNote,
                subtractFeeFromAmount = subtractFeeFromAmount,
                slots = args.slots,
                inputs = args.inputs,
                manualFeeRate = manualFeeRate,
                claimInheritanceTxParam = args.claimInheritanceTxParam,
                antiFeeSniping = viewModel.getAddReceiptState().antiFeeSniping
            )
            if (state.addressType.isTaproot() && state.scriptNode != null) {
                if (state.isValueKeySetDisable) {
                    transactionConfirmViewModel.checkMiniscriptSigningPolicy()
                } else {
                    transactionConfirmViewModel.checkMiniscriptSigningPaths()
                }
            } else if (state.addressType.isTaproot()) {
                transactionConfirmViewModel.checkShowTaprootDraftTransaction()
            } else {
                transactionConfirmViewModel.handleConfirmEvent(true)
            }
        }
    }

    private fun showEventError(message: String) {
        NCToastMessage(this).showError(message)
    }

    companion object {

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
        ) {
            activityContext.startActivity(
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
                    claimInheritanceTxParam = claimInheritanceTxParam
                ).buildIntent(activityContext)
            )
        }

    }

}