package com.nunchuk.android.compose

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun ActionItem(
    title: String,
    subtitle: String = "",
    isEnable: Boolean = true,
    @DrawableRes iconId: Int,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(enabled = isEnable) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            painter = painterResource(id = iconId),
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
                .alpha(if (isEnable) 1f else 0.6f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = NunchukTheme.typography.body
                )
            }
        }

        if (isEnable) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = "",
            )
        }
    }
}

@PreviewLightDark
@Composable
fun PreviewActionItem() {
    ActionItem(
        title = "Add COLDCARD via QR",
        subtitle = "Scan QR code",
        iconId = R.drawable.ic_qr,
    )
}
