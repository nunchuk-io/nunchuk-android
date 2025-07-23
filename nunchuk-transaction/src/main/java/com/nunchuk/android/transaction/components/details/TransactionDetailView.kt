package com.nunchuk.android.transaction.components.details

import android.text.format.DateUtils
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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.MODE_VIEW_ONLY
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.miniscript.ScriptMode
import com.nunchuk.android.compose.miniscript.ScriptNodeData
import com.nunchuk.android.compose.miniscript.ScriptNodeTree
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.getPendingSignatures
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.core.util.hasChangeIndex
import com.nunchuk.android.core.util.isPendingSignatures
import com.nunchuk.android.core.util.isRejected
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
import com.nunchuk.android.transaction.components.details.view.AmountView
import com.nunchuk.android.transaction.components.details.view.ChangeAddressView
import com.nunchuk.android.transaction.components.details.view.PendingSignatureStatusView
import com.nunchuk.android.transaction.components.details.view.TimeLockUtilView
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.formatByWeek
import com.nunchuk.android.utils.simpleWeekDayYearFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailView(
    args: TransactionDetailsArgs,
    state: TransactionDetailsState = TransactionDetailsState(),
    miniscriptUiState: TransactionMiniscriptUiState = TransactionMiniscriptUiState(),
    onShowMore: () -> Unit = {},
    onSignClick: (SignerModel) -> Unit = {},
    onBroadcastClick: () -> Unit = {},
    onViewOnBlockExplorer: () -> Unit = {},
    onManageCoinClick: () -> Unit = {},
    onEditNote: () -> Unit = {},
    onEditChangeCoin: (UnspentOutput) -> Unit = {},
    onCopyText: (String) -> Unit = {},
) {
    var showDetail by rememberSaveable { mutableStateOf(false) }
    var showInputCoin by rememberSaveable { mutableStateOf(false) }
    var isExpanded by rememberSaveable(state.isValueKeySetDisable) { mutableStateOf(state.isValueKeySetDisable) }
    val transaction =
        if (args.inheritanceClaimTxDetailInfo != null) state.transaction.copy(changeIndex = args.inheritanceClaimTxDetailInfo.changePos) else state.transaction
    val outputs = if (transaction.isReceive) {
        transaction.receiveOutputs
    } else {
        transaction.outputs.filterIndexed { index, _ -> index != transaction.changeIndex }
    }
    val signerMap by remember(state.signers) {
        derivedStateOf {
            state.signers.associateBy { it.fingerPrint }
        }
    }
    val hasChange: Boolean = transaction.hasChangeIndex()
    val changeCoin = state.coins.find { it.vout == transaction.changeIndex }
    val keySetMap = remember(state.transaction, state.defaultKeySetIndex) {
        transaction.keySetStatus.withIndex().associate { it.index to it.value }
    }
    val firstKeySet = if (!state.isValueKeySetDisable) keySetMap[state.defaultKeySetIndex] else null
    val isMiniscriptTaprootKeyPathTransaction =
        state.addressType.isTaproot() && miniscriptUiState.scriptNode != null && state.defaultKeySetIndex == 0

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
                if ((transaction.status.canBroadCast() || transaction.status.isRejected())
                    && args.inheritanceClaimTxDetailInfo == null && state.userRole.isObserver.not()
                    && isServerBroadcastTime(transaction, state.serverTransaction).not()
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        enabled = !miniscriptUiState.isTimelockedActive,
                        onClick = onBroadcastClick
                    ) {
                        Text(
                            text = if (transaction.status.isRejected()) stringResource(R.string.nc_re_broadcast_transaction) else stringResource(
                                R.string.nc_transaction_broadcast
                            ),
                        )
                    }
                } else if (transaction.status.hadBroadcast()) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = onViewOnBlockExplorer
                    ) {
                        Text(
                            text = stringResource(R.string.nc_transaction_view_blockchain),
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
                        isTimelockedActive = miniscriptUiState.isTimelockedActive,
                        transaction = state.transaction,
                        serverTransaction = state.serverTransaction,
                        allTxCoins = state.coins,
                        outputs = outputs,
                        userRole = state.userRole,
                        showDetail = showDetail,
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

                    itemsIndexed(outputs) { index, output ->
                        com.nunchuk.android.transaction.components.details.view.TransactionOutputItem(
                            savedAddresses = state.savedAddress,
                            output = output,
                            onCopyText = onCopyText,
                            hideFiatCurrency = state.hideFiatCurrency
                        )

                        if (!transaction.isReceive || index < outputs.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.backgroundMidGray,
                                thickness = 1.dp
                            )
                        }
                    }

                    if (!transaction.isReceive) {
                        item {
                            TransactionEstimateFee(
                                modifier = Modifier.padding(top = 24.dp),
                                fee = transaction.fee,
                                hideFiatCurrency = state.hideFiatCurrency
                            )
                        }

                        item {
                            TransactionTotalAmount(
                                modifier = Modifier.padding(top = 16.dp),
                                total = transaction.totalAmount,
                                hideFiatCurrency = state.hideFiatCurrency
                            )
                        }
                    }

                    if (hasChange && args.inheritanceClaimTxDetailInfo == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxWidth()
                                    .background(color = MaterialTheme.colorScheme.backgroundMidGray)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.nc_transaction_change_address),
                                    style = NunchukTheme.typography.titleSmall,
                                )

                                if (changeCoin != null) {
                                    Text(
                                        text = stringResource(R.string.nc_edit),
                                        style = NunchukTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.Underline
                                        ),
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .clickable(onClick = { onEditChangeCoin(changeCoin) }),
                                    )
                                }
                            }
                        }

                        item {
                            ChangeAddressView(
                                txOutput = transaction.outputs[transaction.changeIndex],
                                output = changeCoin,
                                tags = state.tags,
                                hideFiatCurrency = state.hideFiatCurrency
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .padding(top = if (transaction.isReceive) 0.dp else 16.dp)
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
                        items(
                            state.txInputCoins.take(30),
                            key = { "${it.txid} - ${it.vout}" },
                        ) { input ->
                            PreviewCoinCard(
                                output = input,
                                mode = MODE_VIEW_ONLY,
                                tags = state.tags
                            )
                        }

                        if (state.txInputCoins.size > 30) {
                            item {
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    text = stringResource(
                                        R.string.nc_more_address,
                                        state.txInputCoins.size - 30
                                    ),
                                    style = NunchukTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
                                )
                            }
                        }
                    }
                }

                if (miniscriptUiState.isTimelockedActive) {
                    item {
                        TimeLockUtilView(
                            lockedTime = miniscriptUiState.lockedTime,
                            lockedBase = miniscriptUiState.lockedBase
                        )
                    }
                }

                if (!state.transaction.isReceive && miniscriptUiState.scriptNode != null && state.signerMap.isNotEmpty() && !isMiniscriptTaprootKeyPathTransaction) {
                    item {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 24.dp
                            )
                        ) {
                            ScriptNodeTree(
                                node = miniscriptUiState.scriptNode,
                                data = ScriptNodeData(
                                    mode = ScriptMode.SIGN,
                                    signers = state.signerMap,
                                    showBip32Path = false,
                                    signedSigners = transaction.signers,
                                    satisfiableMap = miniscriptUiState.satisfiableMap,
                                    topLevelDisableNode = miniscriptUiState.topLevelDisableNode
                                ),
                                onChangeBip32Path = { _, _ -> },
                                onActionKey = { _, signer ->
                                    signer?.let(onSignClick)
                                }
                            )
                        }
                    }
                }
                // taproot key set view
                else if (transaction.keySetStatus.isNotEmpty() && !isMiniscriptTaprootKeyPathTransaction) {
                    if (firstKeySet != null) {
                        item {
                            KeySetView(
                                signers = signerMap,
                                keySetIndex = state.defaultKeySetIndex,
                                requiredSignatures = transaction.m,
                                keySet = firstKeySet,
                                onSignClick = onSignClick
                            )
                        }
                    }

                    if (state.isValueKeySetDisable) {
                        item {
                            OtherKeySetView(
                                toggleExpand = {
                                    isExpanded = !isExpanded
                                },
                                count = transaction.keySetStatus.size - 1,
                                isExpanded = isExpanded,
                                isValueKeySetDisable = true
                            )
                        }
                    } else {
                        item {
                            OtherKeySetView(
                                modifier = Modifier.padding(top = 16.dp),
                                toggleExpand = {
                                    isExpanded = !isExpanded
                                },
                                count = transaction.keySetStatus.size - 1,
                                isExpanded = isExpanded,
                                isValueKeySetDisable = false
                            )
                        }
                    }

                    if (isExpanded) {
                        val finalKeySet = if (state.isValueKeySetDisable) {
                            keySetMap.filter { it.key != state.defaultKeySetIndex }
                        } else {
                            keySetMap
                        }
                        finalKeySet.filter { state.isValueKeySetDisable || it.key != state.defaultKeySetIndex }
                            .forEach { (index, keySetStatus) ->
                                item {
                                    KeySetView(
                                        signers = signerMap,
                                        keySetIndex = index,
                                        requiredSignatures = transaction.m,
                                        keySet = keySetStatus,
                                        onSignClick = onSignClick,
                                        showDivider = index < finalKeySet.size.dec(),
                                        isValueKeySetDisable = state.isValueKeySetDisable
                                    )
                                }
                            }
                    }
                }
                // normal transaction signer view
                else if (!transaction.isReceive && args.inheritanceClaimTxDetailInfo == null && state.signers.isNotEmpty()) {
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
                            showValueKey = index < transaction.m && state.addressType.isTaproot() && !state.isValueKeySetDisable && miniscriptUiState.scriptNode == null,
                            isSigned = transaction.signers.isNotEmpty() && transaction.signers[signer.fingerPrint] ?: false,
                            canSign = !transaction.status.signDone(),
                            onSignClick = onSignClick,
                        ) {
                            val isSigned =
                                transaction.signers.isNotEmpty() && transaction.signers[signer.fingerPrint] ?: false
                            val serverTransaction = state.serverTransaction
                            val spendingLimitMessage =
                                serverTransaction?.spendingLimitMessage.orEmpty()
                            val cosignedTime = serverTransaction?.signedInMilis ?: 0L
                            if (serverTransaction?.isCosigning == true) {
                                Text(
                                    text = stringResource(R.string.nc_co_signing_in_progress),
                                    style = NunchukTheme.typography.bodySmall,
                                    color = colorResource(R.color.nc_beeswax_dark),
                                )
                            } else if (spendingLimitMessage.isNotEmpty()) {
                                Text(
                                    text = serverTransaction?.spendingLimitMessage.orEmpty(),
                                    style = NunchukTheme.typography.bodySmall,
                                    color = colorResource(R.color.nc_beeswax_dark),
                                )
                            } else if (cosignedTime > 0L && isSigned.not() && transaction.status.isPendingSignatures()) {
                                val cosignDate = Date(cosignedTime)
                                val content = if (DateUtils.isToday(cosignedTime)) {
                                    "${stringResource(R.string.nc_cosign_at)} [B]${cosignDate.formatByHour()}[/B]"
                                } else {
                                    "${stringResource(R.string.nc_cosign_at)} [B]${cosignDate.formatByHour()} ${cosignDate.formatByWeek()}[/B]"
                                }
                                NcHighlightText(
                                    text = content,
                                    style = NunchukTheme.typography.bodySmall.copy(
                                        color = colorResource(R.color.nc_beeswax_dark)
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionHeader(
    args: TransactionDetailsArgs,
    isTimelockedActive: Boolean,
    transaction: Transaction,
    serverTransaction: ServerTransaction?,
    allTxCoins: List<UnspentOutput>,
    outputs: List<TxOutput>,
    userRole: AssistedWalletRole,
    showDetail: Boolean,
    onShowDetails: () -> Unit,
    onManageCoinClick: () -> Unit,
) {
    val sendToAddress = if (outputs.size >= 2) {
        stringResource(R.string.nc_multiple_addresses)
    } else {
        outputs.firstOrNull()?.first.orEmpty().truncatedAddress()
    }
    val status = if (isTimelockedActive) {
        stringResource(R.string.nc_timelocked)
    } else when (transaction.status) {
        TransactionStatus.PENDING_SIGNATURES -> stringResource(R.string.nc_transaction_pending_signatures)
        TransactionStatus.READY_TO_BROADCAST -> stringResource(R.string.nc_ready_to_broadcast)
        TransactionStatus.PENDING_CONFIRMATION -> stringResource(R.string.nc_transaction_pending_confirmation)
        TransactionStatus.CONFIRMED -> "${transaction.height} ${stringResource(R.string.nc_transaction_confirmations)}"
        TransactionStatus.NETWORK_REJECTED -> stringResource(R.string.nc_transaction_network_rejected)
        TransactionStatus.REPLACED -> stringResource(R.string.nc_transaction_replaced)
        TransactionStatus.PENDING_NONCE -> ""
    }
    val statusColor = if (isTimelockedActive) {
        colorResource(R.color.nc_red_tint_color)
    } else when (transaction.status) {
        TransactionStatus.PENDING_SIGNATURES -> colorResource(R.color.nc_red_tint_color)
        TransactionStatus.READY_TO_BROADCAST -> colorResource(R.color.nc_beeswax_tint)
        TransactionStatus.PENDING_CONFIRMATION -> colorResource(R.color.nc_lavender_tint_color)
        TransactionStatus.CONFIRMED -> colorResource(R.color.nc_denim_tint_color)
        TransactionStatus.NETWORK_REJECTED -> colorResource(R.color.nc_red_tint_color)
        TransactionStatus.REPLACED -> colorResource(R.color.nc_white_color)
        TransactionStatus.PENDING_NONCE -> colorResource(R.color.nc_white_color)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.lightGray)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        if (serverTransaction != null && transaction.status.canBroadCast() && serverTransaction.type == ServerTransactionType.SCHEDULED) {
            val broadcastTime = Date(serverTransaction.broadcastTimeInMilis)
            val scheduleTime = stringResource(
                R.string.nc_broadcast_on,
                broadcastTime.simpleWeekDayYearFormat(),
                broadcastTime.formatByHour()
            )
            Row(
                modifier = Modifier
                    .background(
                        color = statusColor,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_schedule),
                    contentDescription = "Schedule",
                    tint = colorResource(R.color.nc_grey_g7),
                )
                Text(
                    text = scheduleTime,
                    color = colorResource(R.color.nc_grey_g7),
                    style = NunchukTheme.typography.caption,
                )
            }
        } else {
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

        if (args.inheritanceClaimTxDetailInfo == null) {
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
                text = if (showDetail)
                    stringResource(R.string.nc_transaction_less_details)
                else
                    stringResource(R.string.nc_transaction_more_details),
                style = NunchukTheme.typography.title,
            )

            NcIcon(
                painter = painterResource(id = if (showDetail) R.drawable.ic_collapse else R.drawable.ic_expand),
                contentDescription = "Show Details",
            )
        }
    }
}

@Composable
private fun TransactionEstimateFee(
    modifier: Modifier = Modifier,
    fee: Amount,
    hideFiatCurrency: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.nc_fee),
            style = NunchukTheme.typography.body,
        )

        Spacer(modifier = Modifier.weight(1f))

        AmountView(fee, hideFiatCurrency)
    }
}

@Composable
private fun TransactionTotalAmount(
    modifier: Modifier = Modifier,
    total: Amount,
    hideFiatCurrency: Boolean = false
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
            modifier = Modifier.weight(1f),
        )

        AmountView(total, hideFiatCurrency)
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
            inheritanceClaimTxDetailInfo = null
        ),
        state = TransactionDetailsState(
            transaction = Transaction(
                txId = "txId",
                status = TransactionStatus.NETWORK_REJECTED,
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
    )
}