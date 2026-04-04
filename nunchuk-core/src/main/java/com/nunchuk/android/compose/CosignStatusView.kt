package com.nunchuk.android.compose

import android.text.format.DateUtils
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.R
import com.nunchuk.android.model.GroupTransactionState
import com.nunchuk.android.model.transaction.ServerTransaction
import com.nunchuk.android.type.GroupTransactionStatus
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.formatByWeek
import java.util.Date

@Composable
fun CosignStatusView(
    isFreeGroupWallet: Boolean,
    groupTransactionState: GroupTransactionState?,
    serverTransaction: ServerTransaction?,
    isSigned: Boolean,
    isPendingSignatures: Boolean,
) {
    if (isFreeGroupWallet && groupTransactionState != null) {
        FreeGroupCosignStatus(
            groupTransactionState = groupTransactionState,
            isSigned = isSigned,
            isPendingSignatures = isPendingSignatures,
        )
    } else {
        AssistedCosignStatus(
            serverTransaction = serverTransaction,
            isSigned = isSigned,
            isPendingSignatures = isPendingSignatures,
        )
    }
}

@Composable
private fun FreeGroupCosignStatus(
    groupTransactionState: GroupTransactionState,
    isSigned: Boolean,
    isPendingSignatures: Boolean,
) {
    when (groupTransactionState.status) {
        GroupTransactionStatus.COSIGNING -> {
            if (groupTransactionState.message.isNotEmpty()) {
                Text(
                    text = groupTransactionState.message,
                    style = NunchukTheme.typography.bodySmall,
                    color = colorResource(R.color.nc_beeswax_dark),
                )
            }
        }

        GroupTransactionStatus.PENDING_DELAY -> {
            CosignAtTimeText(
                cosignedTime = groupTransactionState.cosignAt,
                isSigned = isSigned,
                isPendingSignatures = isPendingSignatures,
            )
        }

        GroupTransactionStatus.BLOCKED -> {
            if (groupTransactionState.message.isNotEmpty()) {
                Text(
                    text = groupTransactionState.message,
                    style = NunchukTheme.typography.bodySmall,
                    color = colorResource(R.color.nc_beeswax_dark),
                )
            }
        }

        else -> {}
    }
}

@Composable
private fun AssistedCosignStatus(
    serverTransaction: ServerTransaction?,
    isSigned: Boolean,
    isPendingSignatures: Boolean,
) {
    val spendingLimitMessage = serverTransaction?.spendingLimitMessage.orEmpty()
    if (serverTransaction?.isCosigning == true) {
        Text(
            text = stringResource(R.string.nc_co_signing_in_progress),
            style = NunchukTheme.typography.bodySmall,
            color = colorResource(R.color.nc_beeswax_dark),
        )
    } else if (spendingLimitMessage.isNotEmpty()) {
        Text(
            text = spendingLimitMessage,
            style = NunchukTheme.typography.bodySmall,
            color = colorResource(R.color.nc_beeswax_dark),
        )
    } else {
        CosignAtTimeText(
            cosignedTime = serverTransaction?.signedInMilis ?: 0L,
            isSigned = isSigned,
            isPendingSignatures = isPendingSignatures,
        )
    }
}

@Composable
private fun CosignAtTimeText(
    cosignedTime: Long,
    isSigned: Boolean,
    isPendingSignatures: Boolean,
) {
    if (cosignedTime > 0L && !isSigned && isPendingSignatures) {
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
