package com.nunchuk.android.wallet.components.coin.detail.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun LockCoinRow(
    output: UnspentOutput,
    onLockCoin: (isLocked: Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_lock_this_coin),
            style = NunchukTheme.typography.title
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(checked = output.isLocked, onCheckedChange = onLockCoin)
    }
}