package com.nunchuk.android.compose.coin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundPrimary
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.getNearestTimeLock
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.share.miniscript.rememberBlockHeightManager
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.utils.simpleDateFormat
import java.util.Date
import kotlin.math.ceil

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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
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
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                            width = 1.dp
                        )
                        .background(
                            color = MaterialTheme.colorScheme.backgroundPrimary,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
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
fun getTimelockRemainingInfo(
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