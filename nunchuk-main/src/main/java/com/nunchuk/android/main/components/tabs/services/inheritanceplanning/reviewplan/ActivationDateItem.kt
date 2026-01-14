package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ActivationDateItem(
    activationDate: String = "January 1, 2024",
    timeZoneId: String = "",
    editable: Boolean = false,
    isHeightLock: Boolean = false,
    blockHeight: Long? = null,
    onClick: () -> Unit = {}
) {
    val timeZoneDisplay = getTimezoneDisplay(timeZoneId)

    if (isHeightLock && blockHeight != null) {
        // HEIGHT_LOCK UI: Block height on left, date/time on right with vertical divider
        Row(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left column: Block Height
            Column(
                modifier = Modifier
            ) {
                val formattedBlockHeight = NumberFormat.getNumberInstance(Locale.US).format(blockHeight)
                Text(
                    text = formattedBlockHeight,
                    style = NunchukTheme.typography.body,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.nc_block_height),
                    style = NunchukTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Vertical divider
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.3f)
            )

            // Right column: Date/Time (Estimated) and Timezone
            Column(
                modifier = Modifier.weight(1.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = activationDate,
                        style = NunchukTheme.typography.body,
                        color = Color.White
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        text = "(Estimated)",
                        style = NunchukTheme.typography.bodySmall,
                        color = colorResource(R.color.nc_text_disable),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeZoneDisplay,
                    style = NunchukTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    } else {
        // TIME_LOCK UI: Original layout with calendar icon
        Row(
            modifier = Modifier
                .background(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_calendar_light),
                tint = Color.White,
                contentDescription = ""
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = activationDate,
                    style = NunchukTheme.typography.body,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeZoneDisplay,
                    style = NunchukTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            if (editable) {
                Text(
                    modifier = Modifier.clickable {
                        onClick()
                    },
                    text = stringResource(id = R.string.nc_edit),
                    color = colorResource(id = R.color.nc_white_color),
                    style = NunchukTheme.typography.title,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}