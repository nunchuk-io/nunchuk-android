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

package com.nunchuk.android.compose.coin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import com.nunchuk.android.compose.CoinStatusBadge
import com.nunchuk.android.compose.CoinTagGroupView
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundPrimary
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.formatDate
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getNearestTimeLock
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.share.miniscript.rememberBlockHeightManager
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.utils.simpleDateFormat
import java.util.Date
import kotlin.math.ceil

const val MODE_VIEW_ONLY = 1
const val MODE_VIEW_DETAIL = 2
const val MODE_SELECT = 3

@Composable
fun PreviewCoinCard(
    modifier: Modifier = Modifier,
    output: UnspentOutput,
    tags: Map<Int, CoinTag>,
    mode: Int = MODE_VIEW_DETAIL,
    isSelected: Boolean = false,
    onViewCoinDetail: (output: UnspentOutput) -> Unit = {},
    onViewTagDetail: (tag: CoinTag) -> Unit = {},
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    Box(
        modifier = modifier
            .run {
                if (mode == MODE_VIEW_DETAIL) {
                    this.clickable { onViewCoinDetail(output) }
                } else {
                    this
                }
            }) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (LocalView.current.isInEditMode)
                        "${output.amount.value} sats"
                    else
                        output.amount.getBTCAmount(),
                    style = NunchukTheme.typography.title
                )
                if (output.isChange && mode != MODE_VIEW_ONLY) {
                    Text(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                color = MaterialTheme.colorScheme.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = stringResource(R.string.nc_change),
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
                if (output.isLocked && mode != MODE_VIEW_ONLY) {
                    NcIcon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "Lock"
                    )
                }
                if (output.scheduleTime > 0L && mode != MODE_VIEW_ONLY) {
                    NcIcon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_schedule),
                        contentDescription = "Schedule"
                    )
                }
            }
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = output.amount.getCurrencyAmount(),
                style = NunchukTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textSecondary
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (output.time > 0L) {
                    Text(
                        text = output.time.formatDate(),
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                } else {
                    Text(
                        text = "--/--/--",
                        style = NunchukTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textSecondary
                    )
                }

                if (mode != MODE_VIEW_ONLY || output.status == CoinStatus.INCOMING_PENDING_CONFIRMATION) {
                    CoinStatusBadge(output)
                }
            }

            // Timelock timeline
            if (output.timelocks.isNotEmpty()) {
                TimelockTimeline(
                    modifier = Modifier.padding(top = 8.dp),
                    output = output
                )
            }

            if (output.tags.isNotEmpty() || output.memo.isNotEmpty()) {
                CoinTagGroupView(
                    modifier = Modifier.padding(top = 4.dp),
                    note = output.memo,
                    tagIds = output.tags,
                    tags = tags,
                    onViewTagDetail = onViewTagDetail
                )
            }
        }
        if (mode == MODE_SELECT) {
            NcCheckBox(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp),
                checked = isSelected,
                onCheckedChange = { select ->
                    onSelectCoin(output, select)
                })
        } else if (mode == MODE_VIEW_DETAIL) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp),
                onClick = { onViewCoinDetail(output) }) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_arrow),
                    contentDescription = ""
                )
            }
        }
    }
}

@Composable
fun TimelockTimeline(
    modifier: Modifier = Modifier,
    output: UnspentOutput,
) {
    val blockHeightManager = rememberBlockHeightManager()
    val currentBlockHeight by blockHeightManager.state.collectAsStateWithLifecycle()
    val nearestTimelock = output.getNearestTimeLock(currentBlockHeight)
    val isUnderLocked = nearestTimelock != null
    val lastLockedTime = output.timelocks.last() // we used it when isUnderLocked is false
    val timeToShow = nearestTimelock ?: lastLockedTime

    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.lightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // Header with clock icon and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_timer),
                contentDescription = "Timelock",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.nc_timelock_timeline),
                style = NunchukTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
            )
        }

        Text(
            text = if (output.lockBased == MiniscriptTimelockBased.TIME_LOCK) {
                Date(
                    timeToShow * 1000L
                ).simpleDateFormat()
            } else {
                timeToShow.formatDecimalWithoutZero()
            },
            style = NunchukTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(vertical = 8.dp)
        ) {
            val (dotIcon, lockIcon, arrowIcon, remainingText, unlockedText, leftTrack, rightTrack) = createRefs()

            // 1. Draw the rectangle line under layer (background track) - split into two parts
            // Left track segment (from dot to lock)
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUnderLocked) MaterialTheme.colorScheme.textPrimary else MaterialTheme.colorScheme.strokePrimary,
                        shape = RoundedCornerShape(1.5.dp)
                    )
                    .height(4.dp)
                    .padding(start = 4.dp)
                    .constrainAs(leftTrack) {
                        start.linkTo(dotIcon.end)
                        end.linkTo(lockIcon.start)
                        width = Dimension.fillToConstraints
                        centerVerticallyTo(parent)
                    }
            )

            // Right track segment (from lock to arrow)
            Box(
                modifier = Modifier
                    .background(
                        color = if (isUnderLocked) MaterialTheme.colorScheme.strokePrimary else MaterialTheme.colorScheme.textPrimary,
                        shape = RoundedCornerShape(1.5.dp)
                    )
                    .height(4.dp)
                    .padding(end = 12.dp)
                    .constrainAs(rightTrack) {
                        start.linkTo(lockIcon.end)
                        end.linkTo(arrowIcon.start)
                        width = Dimension.fillToConstraints
                        centerVerticallyTo(parent)
                    }
            )

            // 2. Draw the left circle on the left
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.backgroundPrimary,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary,
                        shape = CircleShape
                    )
                    .constrainAs(dotIcon) {
                        start.linkTo(parent.start)
                        centerVerticallyTo(parent)
                    }
            )

            // 3. Draw the lock icon in the center
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.backgroundPrimary,
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.strokePrimary,
                        shape = CircleShape
                    )
                    .constrainAs(lockIcon) {
                        centerHorizontallyTo(parent)
                        centerVerticallyTo(parent)
                    },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Lock",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.controlFillPrimary
                )
            }

            // 4. Draw the arrow icon on the right
            NcIcon(
                painter = painterResource(id = R.drawable.ic_caret_right),
                contentDescription = "Arrow",
                modifier = Modifier
                    .constrainAs(arrowIcon) {
                        end.linkTo(parent.end)
                        centerVerticallyTo(parent)
                    },
                tint = if (isUnderLocked) MaterialTheme.colorScheme.strokePrimary else MaterialTheme.colorScheme.textPrimary
            )

            // 5. Show text "Unlocked" between lock icon and arrow (when isUnderLocked == false)
            if (!isUnderLocked) {
                Text(
                    text = stringResource(R.string.nc_unlocked),
                    style = NunchukTheme.typography.caption.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .constrainAs(unlockedText) {
                            start.linkTo(lockIcon.end, margin = 8.dp)
                            end.linkTo(arrowIcon.start, margin = 8.dp)
                            centerVerticallyTo(parent)
                        }
                )
            } else {
                // 6. Show text remaining time between dot and lock icon (when isUnderLocked == true)
                val remainingInfo =
                    getTimelockRemainingInfo(nearestTimelock, output.lockBased, currentBlockHeight)
                Text(
                    text = remainingInfo,
                    style = NunchukTheme.typography.caption,
                    color = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier
                        .border(
                            color = MaterialTheme.colorScheme.textPrimary,
                            shape = RoundedCornerShape(20.dp),
                            width = 1.dp
                        )
                        .background(
                            color = MaterialTheme.colorScheme.backgroundPrimary,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .constrainAs(remainingText) {
                            start.linkTo(dotIcon.end, margin = 8.dp)
                            end.linkTo(lockIcon.start, margin = 8.dp)
                            centerVerticallyTo(parent)
                        }
                )
            }
        }
    }
}

/**
 * Gets remaining time or blocks info as a formatted string
 */
@Composable
private fun getTimelockRemainingInfo(
    nearestTimelock: Long,
    lockBased: MiniscriptTimelockBased,
    currentBlockHeight: Int
): String {
    return when (lockBased) {
        MiniscriptTimelockBased.TIME_LOCK -> {
            val currentTime = System.currentTimeMillis() / 1000L
            val remainingSeconds = nearestTimelock - currentTime
            val remainingDays = ceil(remainingSeconds / 86400.0).toInt()

            when {
                remainingDays > 0 -> pluralStringResource(
                    R.plurals.nc_days_left,
                    remainingDays,
                    remainingDays
                )
                else -> {
                    val remainingHours = (remainingSeconds / 3600).toInt()
                    if (remainingHours > 0) pluralStringResource(
                        R.plurals.nc_plural_hour,
                        remainingHours,
                        remainingHours
                    ) else "Less than 1 hour left"
                }
            }
        }

        MiniscriptTimelockBased.HEIGHT_LOCK -> {
            val remainingBlocks = nearestTimelock.toInt() - currentBlockHeight
            pluralStringResource(
                R.plurals.nc_blocks_left,
                remainingBlocks,
                remainingBlocks
            )
        }

        else -> ""
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview() {
    NunchukTheme {
        PreviewCoinCard(
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = true,
                scheduleTime = System.currentTimeMillis(),
                isChange = true,
                time = System.currentTimeMillis(),
                tags = setOf(1, 2, 3, 4),
                memo = "Send to Bob on Silk Road",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION
            ),
            tags = emptyMap(),
            mode = MODE_VIEW_ONLY
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview2() {
    NunchukTheme {
        PreviewCoinCard(
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = false,
                scheduleTime = System.currentTimeMillis(),
                time = System.currentTimeMillis() / 1000,
                tags = setOf(),
                memo = "",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION,
                timelocks = listOf(System.currentTimeMillis() / 1000 + 432000), // 5 days from now
                lockBased = MiniscriptTimelockBased.TIME_LOCK
            ),
            tags = emptyMap()
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewCoinCardPreview3() {
    NunchukTheme {
        PreviewCoinCard(
            modifier = Modifier
                .background(color = Color.White, shape = RoundedCornerShape(12.dp)),
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = false,
                scheduleTime = System.currentTimeMillis() / 1000,
                time = System.currentTimeMillis() / 1000,
                tags = setOf(),
                memo = "",
                status = CoinStatus.OUTGOING_PENDING_CONFIRMATION,
                timelocks = listOf(1000000L), // Block height timelock
                lockBased = MiniscriptTimelockBased.HEIGHT_LOCK
            ),
            tags = emptyMap(),
            mode = MODE_SELECT,
            isSelected = true
        )
    }
}