package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TransactionNoteView
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.util.MAX_FRACTION_DIGITS
import com.nunchuk.android.core.util.USD_FRACTION_DIGITS
import com.nunchuk.android.core.util.canBroadCast
import com.nunchuk.android.core.util.formatDecimal
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.fromBTCToCurrency
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getLbtcAmount
import com.nunchuk.android.core.util.getDisplayCurrency
import com.nunchuk.android.core.util.getFormatDate
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.core.util.truncatedAddress
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.transaction.ExtendedTransaction
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.model.transaction.ServerTransactionType
import com.nunchuk.android.share.miniscript.currentBlock
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.Utils
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.weekDayYearFormat
import com.nunchuk.android.wallet.R
import java.util.Date

@Composable
internal fun TransactionList(
    items: List<ExtendedTransaction>,
    hideWalletDetail: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    listState: LazyListState = rememberLazyListState(),
    isStableWallet: Boolean = false,
    usdtAssetId: String = "",
    onClick: (Transaction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        items(items = items, key = { it.transaction.txId }) { extended ->
            TransactionRow(
                extended = extended,
                hideWalletDetail = hideWalletDetail,
                isStableWallet = isStableWallet,
                usdtAssetId = usdtAssetId,
                onClick = { if (!hideWalletDetail) onClick(extended.transaction) },
            )
        }
    }
}

@Composable
internal fun TransactionRow(
    extended: ExtendedTransaction,
    hideWalletDetail: Boolean,
    isStableWallet: Boolean = false,
    usdtAssetId: String = "",
    onClick: () -> Unit,
) {
    val transaction = extended.transaction
    val isTimelockedActive = isTimelockedActive(
        lockedTime = extended.lockedTime,
        lockedBase = extended.lockedBase,
        currentBlock = LocalContext.current.currentBlock,
    )

    val stableDisplay = if (isStableWallet) {
        rememberStableTxDisplay(transaction, usdtAssetId, extended.hideFiatCurrency, hideWalletDetail)
    } else null

    val isReceive = transaction.isReceive
    val amountColor = if (isReceive) colorResource(com.nunchuk.android.wallet.R.color.nc_slime_dark)
    else MaterialTheme.colorScheme.textPrimary

    val receiverText = when {
        isReceive && transaction.receiveOutputs.size > 1 ->
            stringResource(R.string.nc_multiple_addresses)

        isReceive -> Utils.maskValue(
            transaction.receiveOutputs.firstOrNull()?.first.orEmpty().truncatedAddress(),
            hideWalletDetail,
        )

        else -> {
            val outs = transaction.outputs.filter { !it.isChange }
            if (outs.size > 1) stringResource(R.string.nc_multiple_addresses)
            else Utils.maskValue(
                outs.firstOrNull()?.first.orEmpty().truncatedAddress(),
                hideWalletDetail,
            )
        }
    }

    val btcAmount = stableDisplay?.tokenAmount ?: if (isReceive) {
        Utils.maskValue(transaction.totalAmount.getBTCAmount(), hideWalletDetail)
    } else {
        Utils.maskValue("- ${transaction.totalAmount.getBTCAmount()}", hideWalletDetail)
    }
    val cashAmount = stableDisplay?.cashAmount ?: if (extended.hideFiatCurrency) "" else {
        if (isReceive) {
            Utils.maskValue(transaction.totalAmount.getCurrencyAmount(), hideWalletDetail)
        } else {
            Utils.maskValue("- ${transaction.totalAmount.getCurrencyAmount()}", hideWalletDetail)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (isReceive) R.string.nc_transaction_receive_at
                    else R.string.nc_transaction_send_to
                ),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )
            Spacer(Modifier.weight(1f))
            if (transaction.replacedTxid.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.strokePrimary, RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = stringResource(R.string.nc_rbf),
                        style = NunchukTheme.typography.captionTitle.copy(
                            color = MaterialTheme.colorScheme.textPrimary
                        ),
                    )
                }
                Spacer(Modifier.size(4.dp))
            }
            StatusBadge(
                transaction = transaction,
                serverTransaction = extended.serverTransaction,
                isTimelockedActive = isTimelockedActive,
            )
        }
        Spacer(Modifier.size(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = receiverText,
                style = NunchukTheme.typography.body,
                maxLines = 1,
            )
            Text(
                text = btcAmount,
                style = NunchukTheme.typography.title.copy(color = amountColor),
            )
        }
        Spacer(Modifier.size(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = transaction.getFormatDate(),
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary
                ),
            )
            Spacer(Modifier.weight(1f))
            if (cashAmount.isNotEmpty()) {
                Text(
                    text = cashAmount,
                    style = NunchukTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.textPrimary
                    ),
                )
            }
        }
        if (transaction.memo.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            TransactionNoteView(
                modifier = Modifier
                    .border(1.dp, MaterialTheme.colorScheme.strokePrimary, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                note = transaction.memo,
            )
        }
    }
}

@Composable
private fun StatusBadge(
    transaction: Transaction,
    serverTransaction: ServerTransaction?,
    isTimelockedActive: Boolean,
) {
    val isScheduled = serverTransaction != null && transaction.status.canBroadCast() &&
            serverTransaction.type == ServerTransactionType.SCHEDULED

    val (label, bgColor) = when {
        isTimelockedActive ->
            stringResource(R.string.nc_timelocked) to colorResource(R.color.nc_fill_pink)

        isScheduled && serverTransaction!!.broadcastTimeInMilis > 0L -> {
            val date = Date(serverTransaction.broadcastTimeInMilis)
            "${date.formatByHour()} ${date.weekDayYearFormat()}" to Color(0xFFEAEAEA)
        }

        else -> when (transaction.status) {
            TransactionStatus.PENDING_SIGNATURES ->
                stringResource(R.string.nc_transaction_pending_signatures) to
                        colorResource(R.color.nc_red_tint_color)

            TransactionStatus.READY_TO_BROADCAST ->
                stringResource(R.string.nc_ready_to_broadcast) to
                        colorResource(R.color.nc_beeswax_tint)

            TransactionStatus.PENDING_CONFIRMATION ->
                stringResource(R.string.nc_transaction_pending_confirmation) to
                        colorResource(R.color.nc_lavender_tint_color)

            TransactionStatus.CONFIRMED ->
                "${transaction.height} ${stringResource(R.string.nc_transaction_confirmations)}" to
                        colorResource(R.color.nc_denim_tint_color)

            TransactionStatus.NETWORK_REJECTED ->
                stringResource(R.string.nc_transaction_network_rejected) to
                        colorResource(R.color.nc_red_tint_color)

            TransactionStatus.REPLACED ->
                stringResource(R.string.nc_transaction_replaced) to Color.Transparent

            TransactionStatus.PENDING_NONCE -> "" to Color.Transparent
        }
    }

    if (label.isEmpty()) return

    val labelColor = Color(0xFF031F2B)
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isScheduled && !isTimelockedActive) {
            Icon(
                painter = painterResource(R.drawable.ic_schedule),
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.size(4.dp))
        }
        Text(
            text = label,
            style = NunchukTheme.typography.captionTitle.copy(color = labelColor),
        )
    }
}

private fun isTimelockedActive(
    lockedTime: Long,
    lockedBase: MiniscriptTimelockBased,
    currentBlock: Int,
): Boolean = lockedBase != MiniscriptTimelockBased.NONE && lockedTime > 0 && when (lockedBase) {
    MiniscriptTimelockBased.TIME_LOCK -> System.currentTimeMillis() / 1000 < lockedTime
    MiniscriptTimelockBased.HEIGHT_LOCK -> currentBlock > 0 && currentBlock < lockedTime
    else -> false
}

private data class StableTxDisplay(
    val tokenAmount: String,
    val cashAmount: String,
)

@Composable
private fun rememberStableTxDisplay(
    transaction: Transaction,
    usdtAssetId: String,
    hideFiatCurrency: Boolean,
    hideWalletDetail: Boolean,
): StableTxDisplay {
    val isReceive = transaction.isReceive
    val relevantOutputs: List<TxOutput> = if (isReceive) {
        transaction.receiveOutputs
    } else {
        transaction.outputs.filter { !it.isReceive }
    }

    val totalsByAsset: Map<String, Long> = relevantOutputs.groupBy { it.assetId }
        .mapValues { (_, outs) -> outs.sumOf { it.second.value } }
    val usdtTotal = if (usdtAssetId.isEmpty()) 0L else {
        totalsByAsset.entries
            .firstOrNull { it.key.equals(usdtAssetId, ignoreCase = true) }?.value ?: 0L
    }
    val isUsdt = usdtTotal != 0L
    val total = if (isUsdt) usdtTotal else totalsByAsset.values.sum()
    val amount = Amount(value = total)

    val sign = if (isReceive) "" else "- "
    val tokenRaw = if (isUsdt) "${amount.formatUsdtToken()} USDT" else amount.getLbtcAmount()
    val tokenAmount = Utils.maskValue("$sign$tokenRaw", hideWalletDetail)
    val cashAmount = if (hideFiatCurrency) "" else {
        val cashRaw = if (isUsdt) amount.formatUsdtAsCash() else amount.formatLbtcAsCash()
        Utils.maskValue("$sign$cashRaw", hideWalletDetail)
    }

    return StableTxDisplay(
        tokenAmount = tokenAmount,
        cashAmount = cashAmount,
    )
}

private fun Amount.formatUsdtToken(): String =
    pureBTC().formatDecimalWithoutZero(maxFractionDigits = MAX_FRACTION_DIGITS)

private fun Amount.formatUsdtAsCash(): String =
    "${getDisplayCurrency()}${pureBTC().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"

private fun Amount.formatLbtcAsCash(): String =
    "${getDisplayCurrency()}${pureBTC().fromBTCToCurrency().formatDecimal(maxFractionDigits = USD_FRACTION_DIGITS)}"
