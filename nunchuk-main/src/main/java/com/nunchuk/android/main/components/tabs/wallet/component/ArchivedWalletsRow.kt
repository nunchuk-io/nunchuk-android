package com.nunchuk.android.main.components.tabs.wallet.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundLightGray
import com.nunchuk.android.compose.controlFillTertiary
import com.nunchuk.android.main.R

@Composable
fun ArchivedWalletsRow(
    modifier: Modifier = Modifier,
    count: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.backgroundLightGray,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            painter = painterResource(id = R.drawable.ic_archive),
            contentDescription = "Archived"
        )

        Text(
            text = stringResource(R.string.nc_archive_wallets),
            style = NunchukTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 8.dp).weight(1f)
        )

        Box(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.controlFillTertiary, shape = CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = NunchukTheme.typography.titleSmall,
            )
        }

        NcIcon(
            painter = painterResource(id = R.drawable.ic_arrow),
            contentDescription = "Arrow",
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
@PreviewLightDark
private fun ArchivedWalletsRowPreview() {
    NunchukTheme {
        ArchivedWalletsRow(
            count = 5,
            onClick = {}
        )
    }
}