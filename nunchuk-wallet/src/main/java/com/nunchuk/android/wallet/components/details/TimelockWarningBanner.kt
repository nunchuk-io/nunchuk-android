package com.nunchuk.android.wallet.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillBeewax
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.wallet.R
import kotlin.math.ceil

@Composable
internal fun TimelockWarningBanner(
    nearestTimeLock: Pair<MiniscriptTimelockBased, Long>?,
    currentBlock: Int,
    onViewCoins: () -> Unit,
) {
    nearestTimeLock ?: return
    val (lockBased, lockValue) = nearestTimeLock

    val (isWarning, lockInfo) = when (lockBased) {
        MiniscriptTimelockBased.TIME_LOCK -> {
            val nowSec = System.currentTimeMillis() / 1000L
            val remainingDays = ceil((lockValue - nowSec) / 86400.0).toInt()
            val isWarning = remainingDays < 7
            isWarning to (
                if (remainingDays > 0) stringResource(
                    R.string.nc_timelock_expiring_info,
                    pluralStringResource(R.plurals.nc_day, remainingDays, remainingDays)
                ) else null
            )
        }

        MiniscriptTimelockBased.HEIGHT_LOCK -> {
            val remainingBlocks = lockValue.toInt() - currentBlock
            val isWarning = remainingBlocks < 1008
            isWarning to (
                if (remainingBlocks > 0) stringResource(
                    R.string.nc_timelock_expiring_info,
                    pluralStringResource(R.plurals.nc_block, remainingBlocks, remainingBlocks)
                ) else null
            )
        }

        else -> false to null
    }

    if (lockInfo.isNullOrEmpty()) return

    val viewCoinsLabel = stringResource(R.string.nc_view_coins)
    val bgColor = if (isWarning) MaterialTheme.colorScheme.fillBeewax
    else MaterialTheme.colorScheme.lightGray
    val iconRes = if (isWarning) R.drawable.ic_warning_outline else R.drawable.ic_info_36

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(color = bgColor, shape = RoundedCornerShape(12.dp))
            .clickable { onViewCoins() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.size(20.dp),
        )
        val annotated = buildAnnotatedString {
            val cleaned = lockInfo.trim().removeSuffix(viewCoinsLabel).trim()
            if (cleaned.isNotEmpty()) {
                append(cleaned)
                append(' ')
            }
            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(viewCoinsLabel)
            }
        }
        Text(
            text = annotated,
            style = NunchukTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.textPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
