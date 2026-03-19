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

package com.nunchuk.android.transaction.components.send.confirmation

import android.app.Activity
import android.nfc.tech.IsoDep
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.viewbinding.ViewBinding
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.dialog.NcLoadingDialog
import com.nunchuk.android.core.data.model.ClaimInheritanceTxParam
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.data.model.isOffChainClaim
import com.nunchuk.android.core.manager.ActivityManager
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.nfc.SweepType
import com.nunchuk.android.core.sheet.BottomSheetTooltip
import com.nunchuk.android.core.util.InheritanceClaimTxDetailInfo
import com.nunchuk.android.core.util.copyToClipboard
import com.nunchuk.android.core.util.flowObserver
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.SatsCardSlot
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.nav.args.ClaimTransactionArgs
import com.nunchuk.android.share.satscard.SweepSatscardViewModel
import com.nunchuk.android.share.satscard.observerSweepSatscard
import com.nunchuk.android.transaction.R
import com.nunchuk.android.transaction.components.details.view.AmountView
import com.nunchuk.android.transaction.components.details.view.ChangeAddressView
import com.nunchuk.android.transaction.components.details.view.InspectAddressBottomSheet
import com.nunchuk.android.transaction.components.details.view.TransactionOutputItem
import com.nunchuk.android.transaction.components.send.amount.InputAmountActivity
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.AssignTagEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxErrorEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.CreateTxSuccessEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionError
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.InitRoomTransactionSuccess
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.LoadingEvent
import com.nunchuk.android.transaction.components.send.confirmation.TransactionConfirmEvent.UpdateChangeAddress
import com.nunchuk.android.transaction.components.send.confirmation.tag.AssignTagFragment
import com.nunchuk.android.transaction.components.utils.openTransactionDetailScreen
import com.nunchuk.android.transaction.components.utils.showCreateTransactionError
import com.nunchuk.android.transaction.components.utils.toTitle
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class TransactionConfirmActivity : BaseNfcActivity<ViewBinding>() {

    @Inject
    lateinit var sessionHolder: SessionHolder

    private val args: TransactionConfirmArgs by lazy {
        TransactionConfirmArgs.deserializeFrom(intent).also {
            Timber.d("TransactionConfirmArgs: $it")
        }
    }

    private val viewModel: TransactionConfirmViewModel by viewModels()

    private val sweepSatscardViewModel: SweepSatscardViewModel by viewModels()

    override fun initializeBinding(): ViewBinding = ViewBinding {
        ComposeView(this).apply {
            setContent {
                TransactionConfirmScreen(
                    activity = this@TransactionConfirmActivity,
                    args = args,
                    viewModel = viewModel,
                    sessionHolder = sessionHolder,
                )
            }
        }
    }.also {
        enableEdgeToEdge()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeSweepSatscard()
        viewModel.init(
            walletId = args.walletId,
            txReceipts = args.txReceipts,
            subtractFeeFromAmount = args.subtractFeeFromAmount,
            privateNote = args.privateNote,
            manualFeeRate = args.manualFeeRate,
            slots = args.slots,
            claimInheritanceTxParam = args.claimInheritanceTxParam,
            inputs = args.inputs,
            antiFeeSniping = args.antiFeeSniping
        )
        viewModel.draftTransaction(args.signingPath)
    }

    private fun observeSweepSatscard() {
        observerSweepSatscard(sweepSatscardViewModel, nfcViewModel) { args.walletId }
        flowObserver(nfcViewModel.nfcScanInfo.filter { it.requestCode == REQUEST_SATSCARD_SWEEP_SLOT }) {
            sweepSatscardViewModel.init(args.txReceipts.first().address, args.manualFeeRate)
            sweepSatscardViewModel.handleSweepBalance(
                IsoDep.get(it.tag),
                nfcViewModel.inputCvc.orEmpty(),
                args.slots.toList(),
                args.sweepType
            )
            nfcViewModel.clearScanInfo()
        }
    }

    companion object {

        fun start(
            activityContext: Activity,
            walletId: String,
            availableAmount: Double,
            txReceipts: List<TxReceipt>,
            privateNote: String,
            subtractFeeFromAmount: Boolean = false,
            manualFeeRate: Int = 0,
            sweepType: SweepType = SweepType.NONE,
            slots: List<SatsCardSlot> = emptyList(),
            claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
            inputs: List<UnspentOutput> = emptyList(),
            signingPath: SigningPath? = null,
            actionButtonText: String,
            antiFeeSniping: Boolean
        ) {
            activityContext.startActivity(
                TransactionConfirmArgs(
                    walletId = walletId,
                    availableAmount = availableAmount,
                    txReceipts = txReceipts,
                    privateNote = privateNote,
                    subtractFeeFromAmount = subtractFeeFromAmount,
                    manualFeeRate = manualFeeRate,
                    sweepType = sweepType,
                    slots = slots,
                    claimInheritanceTxParam = claimInheritanceTxParam,
                    inputs = inputs,
                    actionButtonText = actionButtonText,
                    antiFeeSniping = antiFeeSniping,
                    signingPath = signingPath
                ).buildIntent(activityContext)
            )
        }
    }
}

@Composable
private fun TransactionConfirmScreen(
    activity: TransactionConfirmActivity,
    args: TransactionConfirmArgs,
    viewModel: TransactionConfirmViewModel,
    sessionHolder: SessionHolder,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var loadingMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var changeAddress by rememberSaveable { mutableStateOf("") }
    var changeAmount by remember { mutableStateOf(Amount(0)) }
    var outputs by remember { mutableStateOf<List<TxOutput>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CreateTxErrorEvent -> {
                    isLoading = false
                    activity.showCreateTransactionError(event.message)
                }

                is CreateTxSuccessEvent -> {
                    isLoading = false
                    activity.navigator.returnToMainScreen(activity)
                    val transaction = event.transaction
                    if (transaction.isPendingSignatures() && args.claimInheritanceTxParam.isOffChainClaim()) {
                        activity.navigator.openClaimTransactionScreen(
                            activityContext = activity,
                            args = ClaimTransactionArgs(
                                transaction = transaction,
                                masterSignerIds = args.claimInheritanceTxParam?.masterSignerIds.orEmpty(),
                                derivationPaths = args.claimInheritanceTxParam?.derivationPaths.orEmpty(),
                                magic = args.claimInheritanceTxParam?.magicalPhrase.orEmpty(),
                            )
                        )
                    } else {
                        val openedWalletId = event.walletId ?: args.walletId
                        activity.openTransactionDetailScreen(
                            txId = event.transaction.txId,
                            walletId = openedWalletId,
                            roomId = sessionHolder.getActiveRoomIdSafe(),
                            inheritanceClaimTxDetailInfo = takeIf { viewModel.isInheritanceClaimingFlow() }?.let {
                                InheritanceClaimTxDetailInfo(
                                    changePos = event.transaction.changeIndex,
                                    selectedWalletId = args.walletId.takeIf { event.walletId.isNullOrEmpty() },
                                )
                            },
                            transaction = event.transaction.takeIf { event.walletId.isNullOrEmpty() && viewModel.isInheritanceClaimingFlow() }
                        )
                    }
                }

                is UpdateChangeAddress -> {
                    isLoading = false
                    changeAddress = event.address
                    changeAmount = event.amount
                }

                is LoadingEvent -> {
                    isLoading = true
                    loadingMessage =
                        if (event.isClaimInheritance) context.getString(R.string.nc_withdrawal_in_progress) else null
                }

                is InitRoomTransactionError -> {
                    isLoading = false
                    activity.showCreateTransactionError(event.message)
                }

                is InitRoomTransactionSuccess -> {
                    isLoading = false
                    @Suppress("DEPRECATION")
                    ActivityManager.popUntil(InputAmountActivity::class.java, true)
                    if (sessionHolder.isLeaveRoom().not()) {
                        activity.navigator.openRoomDetailActivity(activity, event.roomId)
                    }
                }

                is AssignTagEvent -> {
                    isLoading = false
                    AssignTagFragment.newInstance(event.walletId, event.output, event.tags)
                        .apply {
                            lifecycle.addObserver(object : DefaultLifecycleObserver {
                                override fun onDestroy(owner: LifecycleOwner) {
                                    activity.navigator.returnToMainScreen(activity)
                                    activity.openTransactionDetailScreen(
                                        event.txId,
                                        args.walletId,
                                        sessionHolder.getActiveRoomIdSafe(),
                                    )
                                }
                            })
                        }
                        .show(activity.supportFragmentManager, "AssignTagFragment")
                }

                is TransactionConfirmEvent.DraftTransactionSuccess -> {
                    val coins = if (event.transaction.outputs.size == 1) {
                        event.transaction.outputs
                    } else {
                        if (viewModel.isInheritanceClaimingFlow() && event.transaction.hasChangeIndex()) {
                            event.transaction.outputs.filterIndexed { index, _ -> index != event.transaction.changeIndex }
                        } else {
                            event.transaction.outputs.filter { viewModel.isMyCoin(it) == event.transaction.isReceive }
                        }
                    }
                    outputs = coins
                }

                is TransactionConfirmEvent.AssignTagError,
                is TransactionConfirmEvent.AssignTagSuccess,
                is TransactionConfirmEvent.DraftTaprootTransactionSuccess,
                is TransactionConfirmEvent.ChooseSigningPolicy,
                is TransactionConfirmEvent.AutoSelectSigningPath -> isLoading = false

                is TransactionConfirmEvent.ShowTimeLockNotice,
                is TransactionConfirmEvent.CustomizeTransaction,
                is TransactionConfirmEvent.ChooseSigningPathsSuccess -> Unit
            }
        }
    }

    val fee = uiState.transaction.fee
    val outputAmount = args.txReceipts.sumOf { it.amount }
    val totalAmount = if (args.subtractFeeFromAmount) {
        outputAmount
    } else {
        outputAmount + fee.pureBTC()
    }

    NunchukTheme {
        if (isLoading) {
            NcLoadingDialog(customMessage = loadingMessage)
        }
        TransactionConfirmContent(
            title = args.sweepType.toTitle(
                context,
                stringResource(R.string.nc_transaction_confirm_transaction),
                true
            ),
            confirmButtonText = args.actionButtonText.ifEmpty {
                if (viewModel.isInheritanceClaimingFlow()) {
                    stringResource(R.string.nc_confirm_withdraw_balance)
                } else if (args.sweepType == SweepType.NONE) {
                    stringResource(R.string.nc_transaction_confirm_and_create_transaction)
                } else {
                    stringResource(R.string.nc_confirm_and_sweep)
                }
            },
            outputs = outputs,
            savedAddresses = uiState.savedAddress,
            fee = fee,
            totalAmountBtc = totalAmount.getBTCAmount(),
            totalAmountCurrency = totalAmount.getCurrencyAmount(),
            changeAddress = changeAddress,
            changeAmount = changeAmount,
            isOffChainClaim = viewModel.isOffChainClaimingFlow(),
            privateNote = args.privateNote,
            inputs = args.inputs,
            allTags = uiState.allTags,
            onBackPressed = { activity.finish() },
            onConfirmClick = {
                if (args.slots.isNotEmpty()) {
                    activity.startNfcFlow(BaseNfcActivity.REQUEST_SATSCARD_SWEEP_SLOT)
                } else {
                    viewModel.handleConfirmEvent(
                        keySetIndex = if (args.signingPath != null) 1 else 0,
                        signingPath = args.signingPath,
                    )
                }
            },
            onCopyText = { content ->
                activity.copyToClipboard(label = "Nunchuk", text = content)
                NCToastMessage(activity).showMessage(context.getString(R.string.nc_copied_to_clipboard))
            },
            onEstimatedFeeInfoClick = {
                BottomSheetTooltip.newInstance(
                    title = context.getString(R.string.nc_text_info),
                    message = context.getString(R.string.nc_estimated_fee_tooltip),
                ).show(activity.supportFragmentManager, "BottomSheetTooltip")
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransactionConfirmContent(
    title: String = "",
    confirmButtonText: String = "",
    outputs: List<TxOutput> = emptyList(),
    savedAddresses: Map<String, String> = emptyMap(),
    fee: Amount = Amount(0),
    totalAmountBtc: String = "",
    totalAmountCurrency: String = "",
    changeAddress: String = "",
    changeAmount: Amount = Amount(0),
    isOffChainClaim: Boolean = false,
    privateNote: String = "",
    inputs: List<UnspentOutput> = emptyList(),
    allTags: Map<Int, CoinTag> = emptyMap(),
    onBackPressed: () -> Unit = {},
    onConfirmClick: () -> Unit = {},
    onCopyText: (String) -> Unit = {},
    onEstimatedFeeInfoClick: () -> Unit = {},
) {
    var inspectAddress by rememberSaveable { mutableStateOf<String?>(null) }

    if (inspectAddress != null) {
        InspectAddressBottomSheet(
            address = inspectAddress.orEmpty(),
            onCopy = onCopyText,
            onDismiss = { inspectAddress = null },
        )
    }

    Scaffold(
        modifier = Modifier.navigationBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = title,
                textStyle = NunchukTheme.typography.titleLarge,
                onBackPress = onBackPressed,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onConfirmClick,
            ) {
                Text(text = confirmButtonText)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Send to address header
            Text(
                text = stringResource(R.string.nc_transaction_send_to_address),
                style = NunchukTheme.typography.titleSmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                    .padding(16.dp),
            )

            // Output receipts
            outputs.forEachIndexed { index, output ->
                TransactionOutputItem(
                    savedAddresses = savedAddresses,
                    output = output,
                    onCopyText = onCopyText,
                    onInspectAddress = { inspectAddress = it },
                )
                if (index < outputs.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.backgroundMidGray,
                        thickness = 1.dp,
                    )
                }
            }

            // Estimated fee
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.nc_transaction_estimate_fee),
                        style = NunchukTheme.typography.title,
                    )
                    IconButton(onClick = onEstimatedFeeInfoClick) {
                        NcIcon(
                            painter = painterResource(id = R.drawable.ic_help),
                            contentDescription = "Info",
                        )
                    }
                }
                AmountView(fee)
            }

            // Total amount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.nc_transaction_total_amount),
                    style = NunchukTheme.typography.title,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalAmountBtc,
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        text = totalAmountCurrency,
                        style = NunchukTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            // Change address
            if (!isOffChainClaim && changeAddress.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nc_transaction_change_address),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }

                ChangeAddressView(
                    txOutput = TxOutput(changeAddress, changeAmount),
                    output = null,
                    tags = emptyMap(),
                    onCopyText = onCopyText,
                    onInspectAddress = { inspectAddress = it },
                )
            }

            // Private note
            if (privateNote.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nc_transaction_note),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }

                Text(
                    text = privateNote,
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                )
            }

            // Input coins
            if (inputs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                        .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nc_input_coins),
                        style = NunchukTheme.typography.titleSmall,
                    )
                }
                TransactionConfirmCoinList(inputs, allTags)
            }

            Spacer(modifier = Modifier.padding(bottom = 16.dp))
        }
    }
}

@PreviewLightDark
@Composable
private fun TransactionConfirmContentPreview() {
    TransactionConfirmContent(
        title = "Confirm transaction",
        confirmButtonText = "Confirm and create transaction",
        totalAmountBtc = "0.001 BTC",
        totalAmountCurrency = "$5,400.52",
        fee = Amount(10000),
        privateNote = "Test note",
    )
}
