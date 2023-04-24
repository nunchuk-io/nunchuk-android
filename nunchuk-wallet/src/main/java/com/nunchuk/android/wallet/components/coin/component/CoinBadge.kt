package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun CoinBadge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.background,
    border: Dp = 1.dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                border,
                color = NcColor.whisper,
                shape = RoundedCornerShape(24.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Preview
@Composable
fun CoinBadgePreview() {
    NunchukTheme {
        CoinBadge {
            Text(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                text = stringResource(R.string.nc_change),
                style = NunchukTheme.typography.titleSmall.copy(fontSize = 12.sp)
            )
        }
    }
}