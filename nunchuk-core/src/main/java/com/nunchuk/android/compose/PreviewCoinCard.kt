package com.nunchuk.android.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.type.TransactionStatus
import com.nunchuk.android.utils.formatByHour
import com.nunchuk.android.utils.simpleDateFormat
import java.util.*

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
    onSelectCoin: (output: UnspentOutput, isSelected: Boolean) -> Unit = { _, _ -> }
) {
    Box(modifier = modifier
        .run {
        if (mode == MODE_VIEW_DETAIL) {
            this.clickable { onViewCoinDetail(output) }
        } else {
            this
        }
    }) {
        Column(
            modifier = Modifier
                .background(color = Color.White)
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
                                color = MaterialTheme.colors.background,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .border(
                                1.dp,
                                color = NcColor.whisper,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        text = stringResource(R.string.nc_change),
                        style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                    )
                }
                if (output.isLocked && mode != MODE_VIEW_ONLY) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_lock),
                        tint = MaterialTheme.colors.primary,
                        contentDescription = "Lock"
                    )
                }
                if (output.scheduleTime > 0L && mode != MODE_VIEW_ONLY) {
                    Icon(
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = colorResource(id = R.color.nc_whisper_color),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .size(12.dp),
                        painter = painterResource(id = R.drawable.ic_schedule),
                        tint = MaterialTheme.colors.primary,
                        contentDescription = "Schedule"
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (output.time > 0L) {
                    val date = Date(output.time * 1000L)
                    Text(
                        text = "${date.simpleDateFormat()} at ${date.formatByHour()}",
                        style = NunchukTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "--/--/--",
                        style = NunchukTheme.typography.bodySmall
                    )
                }

                CoinStatusBadge(output)
            }

            if (output.tags.isNotEmpty() || output.memo.isNotEmpty()) {
                CoinTagGroupView(
                    modifier = Modifier.padding(top = 4.dp),
                    note = output.memo,
                    tagIds = output.tags,
                    tags = tags
                )
            }
        }
        if (mode == MODE_SELECT) {
            Checkbox(modifier = Modifier
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
                Icon(painter = painterResource(id = R.drawable.ic_arrow), contentDescription = "", tint = MaterialTheme.colors.primary)
            }
        }
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
                status = TransactionStatus.PENDING_CONFIRMATION
            ),
            tags = emptyMap()
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
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = TransactionStatus.PENDING_CONFIRMATION
            ),
            tags = emptyMap()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCoinCardPreview3() {
    NunchukTheme {
        PreviewCoinCard(
            output = UnspentOutput(
                amount = Amount(1000000L),
                isLocked = false,
                scheduleTime = System.currentTimeMillis() / 1000,
                time = System.currentTimeMillis() / 1000,
                tags = setOf(),
                memo = "",
                status = TransactionStatus.PENDING_CONFIRMATION
            ),
            tags = emptyMap(),
            mode = MODE_SELECT,
            isSelected = true
        )
    }
}