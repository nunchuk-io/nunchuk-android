package com.nunchuk.android.transaction.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.MODE_VIEW_ONLY
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.getPendingSignatures
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.isTaproot
import com.nunchuk.android.core.util.signDone
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.model.byzantine.AssistedWalletRole
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.model.byzantine.isObserver
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.TransactionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailView(
    args: TransactionDetailsArgs,
    state: TransactionDetailsState = TransactionDetailsState(),
    onShowMore: () -> Unit = {},
    onSignClick: (SignerModel) -> Unit = {},
    onBroadcastClick: () -> Unit = {},
    onViewOnBlockExplorer: () -> Unit = {},
    onManageCoinClick: () -> Unit = {},
    onEditNote: () -> Unit = {},
    onCopyText: (String) -> Unit = {},
    onShowFeeTooltip: () -> Unit,
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }
    var showInputCoin by rememberSaveable { mutableStateOf(false) }
    val transaction = state.transaction
    val outputs = if (transaction.isReceive)
        transaction.receiveOutputs else
        transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
    val signerMap by remember(state.signers) {
        derivedStateOf {
            state.signers.associateBy { it.fingerPrint }
        }
    }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_transaction_details),
                    textStyle = NunchukTheme.typography.titleLarge,
                    actions = {
                        IconButton(onClick = onShowMore) {
                            NcIcon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "Back",
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (transaction.status.canBroadCast()
                    && args.isInheritanceClaimingFlow.not() && state.userRole.isObserver.not()
                    && isServerBroadcastTime(transaction, state.serverTransaction).not()
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onBroadcastClick
                    ) {
                        Text(
                            text = stringResource(R.string.nc_transaction_ready_to_broadcast),
                        )
                    }
                } else if (transaction.status.hadBroadcast()) {
                    NcOutlineButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onViewOnBlockExplorer
                    ) {
                        Text(
                            text = stringResource(R.string.nc_view_on_explorer),
                        )
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                item {
                    TransactionHeader(
                        args = args,
                        transaction = state.transaction,
                        allTxCoins = state.coins,
                        outputs = outputs,
                        userRole = state.userRole,
                        onShowDetails = { showDetail = !showDetail },
                        onManageCoinClick = onManageCoinClick
                    )
                }

                if (showDetail) {
                    item {
                        Text(
                            text = if (state.transaction.isReceive)
                                stringResource(R.string.nc_transaction_receive_at)
                            else stringResource(R.string.nc_transaction_send_to),
                            style = NunchukTheme.typography.titleSmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                                .padding(16.dp),
                        )
                    }

                    outputs.forEach { output ->
                        item {
                            TransactionOutputItem(
                                output = output,
                                onCopyText = onCopyText,
                            )
                        }
                    }

                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.backgroundMidGray,
                            thickness = 1.dp
                        )
                    }

                    item {
                        TransactionEstimateFee(
                            fee = transaction.fee,
                            onShowFeeTooltip = onShowFeeTooltip
                        )
                    }

                    item {
                        TransactionTotalAmount(
                            modifier = Modifier.padding(top = 16.dp),
                            total = transaction.totalAmount
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.nc_transaction_note),
                                style = NunchukTheme.typography.titleSmall,
                            )

                            Text(
                                text = stringResource(R.string.nc_edit),
                                style = NunchukTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .clickable(onClick = onEditNote),
                            )
                        }
                    }

                    item {
                        Text(
                            text = transaction.memo.ifBlank { stringResource(R.string.nc_none) },
                            style = NunchukTheme.typography.body,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        )
                    }

                    if (state.txInputCoins.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.nc_show_input_coins),
                                    style = NunchukTheme.typography.titleSmall,
                                    modifier = Modifier.align(Alignment.CenterStart),
                                )
                                CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                    NcSwitch(
                                        checked = showInputCoin,
                                        onCheckedChange = { showInputCoin = it },
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                    )
                                }
                            }
                        }

                    }
                    if (showInputCoin) {
                        items(state.txInputCoins, key = { it.address }) { input ->
                            PreviewCoinCard(
                                output = input,
                                mode = MODE_VIEW_ONLY,
                                tags = state.tags
                            )
                        }
                    }
                }

                if (transaction.keySetStatus.isNotEmpty()) {
                    transaction.keySetStatus.forEachIndexed { index, keySetStatus ->
                        item {
                            KeySetView(
                                signers = signerMap,
                                keySetIndex = index,
                                requiredSignatures = transaction.m,
                                keySet = keySetStatus,
                                onSignClick = onSignClick
                            )
                        }
                    }
                } else if (!transaction.isReceive && !args.isInheritanceClaimingFlow) {
                    item {
                        PendingSignatureStatusView(
                            pendingSigners = transaction.getPendingSignatures(),
                            status = transaction.status
                        )
                    }

                    itemsIndexed(state.signers) { index, signer ->
                        TransactionSignerView(
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .padding(horizontal = 16.dp),
                            signer = signer,
                            showValueKey = index < transaction.m && state.addressType.isTaproot(),
                            isSigned = transaction.signers.isNotEmpty() && transaction.signers[signer.fingerPrint] ?: false,
                            canSign = !transaction.status.signDone(),
                            onSignClick = onSignClick
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun PendingSignatureStatusView(pendingSigners: Int, status: TransactionStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.nc_transaction_member_signers),
            style = NunchukTheme.typography.titleSmall,
        )

        if (!status.hadBroadcast()) {
            if (pendingSigners > 0) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_pending_signatures),
                    contentDescription = "Warning",
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.nc_transaction_pending_signature,
                        pendingSigners,
                        pendingSigners
                    ),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            } else {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = "Check",
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = stringResource(R.string.nc_transaction_enough_signers),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TransactionHeader(
    args: TransactionDetailsArgs,
    transaction: Transaction,
    allTxCoins: List<UnspentOutput>,
    outputs: List<TxOutput>,
    userRole: AssistedWalletRole,
    onShowDetails: () -> Unit,
    onManageCoinClick: () -> Unit,
) {
    val sendToAddress = if (outputs.size >= 2) {
        stringResource(R.string.nc_multiple_addresses)
    } else {
        outputs.firstOrNull()?.first.orEmpty().truncatedAddress()
    }
    val status = when (transaction.status) {
        TransactionStatus.PENDING_SIGNATURES -> stringResource(R.string.nc_transaction_pending_signatures)
        TransactionStatus.READY_TO_BROADCAST -> stringResource(R.string.nc_transaction_ready_to_broadcast)
        TransactionStatus.PENDING_CONFIRMATION -> stringResource(R.string.nc_transaction_pending_confirmation)
        TransactionStatus.CONFIRMED -> "${transaction.height} ${stringResource(R.string.nc_transaction_confirmations)}"
        TransactionStatus.NETWORK_REJECTED -> stringResource(R.string.nc_transaction_network_rejected)
        TransactionStatus.REPLACED -> stringResource(R.string.nc_transaction_replaced)
        TransactionStatus.PENDING_NONCE -> ""
    }
    val statusColor = when (transaction.status) {
        TransactionStatus.PENDING_SIGNATURES -> colorResource(R.color.nc_red_tint_color)
        TransactionStatus.READY_TO_BROADCAST -> colorResource(R.color.nc_beeswax_tint)
        TransactionStatus.PENDING_CONFIRMATION -> colorResource(R.color.nc_lavender_tint_color)
        TransactionStatus.CONFIRMED -> colorResource(R.color.nc_denim_tint_color)
        TransactionStatus.NETWORK_REJECTED -> colorResource(R.color.nc_orange_dark_color)
        TransactionStatus.REPLACED -> colorResource(R.color.nc_white_color)
        TransactionStatus.PENDING_NONCE -> colorResource(R.color.nc_white_color)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                modifier = Modifier
                    .background(
                        color = statusColor,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = status,
                color = colorResource(R.color.nc_grey_g7),
                style = NunchukTheme.typography.caption,
            )

            if (transaction.replacedTxid.isNotEmpty()) {
                Text(
                    modifier = Modifier
                        .background(
                            color = colorResource(R.color.nc_white_color),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    text = stringResource(R.string.nc_replace_by_fee),
                    color = colorResource(R.color.nc_grey_g7),
                    style = NunchukTheme.typography.caption,
                )
            }
        }

        Row(modifier = Modifier.padding(top = 24.dp)) {
            Text(
                text = stringResource(
                    if (transaction.isReceive) R.string.nc_transaction_receive_at else R.string.nc_transaction_send_to,
                ),
                style = NunchukTheme.typography.body,
            )

            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = sendToAddress,
                style = NunchukTheme.typography.body.copy(fontWeight = FontWeight.Bold),
            )
        }

        Text(
            text = transaction.totalAmount.getBTCAmount(),
            style = NunchukTheme.typography.heading,
            modifier = Modifier.padding(top = 4.dp),
        )

        if (args.isInheritanceClaimingFlow.not()) {
            Text(
                text = transaction.getFormatDate(),
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        if (allTxCoins.isNotEmpty() && transaction.status.hadBroadcast() && userRole.isKeyHolderLimited.not()) {
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clickable(onClick = onManageCoinClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = stringResource(R.string.nc_manage_coin),
                    style = NunchukTheme.typography.title,
                )

                NcIcon(
                    painter = painterResource(id = R.drawable.ic_arrow),
                    contentDescription = "Manage Coin",
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 24.dp),
            color = MaterialTheme.colorScheme.backgroundMidGray,
            thickness = 1.dp
        )

        Row(
            modifier = Modifier
                .clickable(onClick = onShowDetails)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.nc_transaction_more_details),
                style = NunchukTheme.typography.title,
            )

            NcIcon(
                painter = painterResource(id = R.drawable.ic_expand),
                contentDescription = "Show Details",
            )
        }
    }
}

@Composable
private fun TransactionEstimateFee(
    fee: Amount,
    onShowFeeTooltip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.nc_transaction_estimate_fee),
            style = NunchukTheme.typography.body,
        )

        NcIcon(
            painter = painterResource(id = R.drawable.ic_help),
            contentDescription = "Info",
            modifier = Modifier
                .padding(start = 4.dp)
                .clickable(onClick = onShowFeeTooltip),
        )

        Spacer(modifier = Modifier.weight(1f))

        AmountView(fee)
    }
}

@Composable
private fun TransactionTotalAmount(
    modifier: Modifier = Modifier,
    total: Amount
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.nc_transaction_total_amount),
            style = NunchukTheme.typography.body,
        )

        Spacer(modifier = Modifier.weight(1f))

        AmountView(total)
    }
}

@Composable
private fun TransactionOutputItem(
    output: TxOutput,
    onCopyText: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = { onCopyText(output.first) }),
            text = output.first,
            style = NunchukTheme.typography.title,
        )

        AmountView(output.second)
    }
}

@Composable
private fun AmountView(amount: Amount) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = amount.getBTCAmount(),
            style = NunchukTheme.typography.title,
        )

        Text(
            text = amount.getCurrencyAmount(),
            style = NunchukTheme.typography.bodySmall,
        )
    }
}

private fun isServerBroadcastTime(
    transaction: Transaction,
    serverTransaction: ServerTransaction?
): Boolean {
    return serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED && serverTransaction.broadcastTimeInMilis > 0L
}

@PreviewLightDark
@Composable
private fun TransactionDetailViewPreview() {
    TransactionDetailView(
        args = TransactionDetailsArgs(
            walletId = "walletId",
            txId = "txId",
            isInheritanceClaimingFlow = false
        ),
        state = TransactionDetailsState(
            transaction = Transaction(
                txId = "txId",
                status = TransactionStatus.READY_TO_BROADCAST,
                isReceive = false,
                receiveOutputs = emptyList(),
                outputs = listOf(
                    TxOutput(
                        first = "tb1qgu4hrgq6elva2px86xefkkhsjkeh8a5sellryg",
                        second = Amount(value = 10101),
                    ),
                    TxOutput(
                        first = "tb1qgu4hrgq6elva2px86xefkkhsjkeh8a5sellryg",
                        second = Amount(value = 1101),
                    )
                ),
                changeIndex = 0,
                height = 1,
                replacedTxid = "replacedTxid",
            ),
            txInputCoins = listOf(
                UnspentOutput(
                    txid = "tb1qgu4hrgq6elva2px86xefkkhsjkeh8a5sellryg"
                )
            )
        ),
        onShowFeeTooltip = {},
    )
}