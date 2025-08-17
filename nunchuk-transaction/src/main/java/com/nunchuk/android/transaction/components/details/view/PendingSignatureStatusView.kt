package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.hadBroadcast
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.TransactionStatus

@Composable
fun PendingSignatureStatusView(pendingSigners: Int, status: TransactionStatus, isTaproot: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
    ) {
        Text(
            text = if (isTaproot) stringResource(R.string.nc_key_path) else
                stringResource(R.string.nc_transaction_member_signers),
            style = NunchukTheme.typography.titleSmall,
        )

        if (!status.hadBroadcast()) {
            if (pendingSigners > 0) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_pending_signatures),
                    contentDescription = "Warning",
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.nc_transaction_pending_signature,
                        pendingSigners,
                        pendingSigners
                    ),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            } else {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_check_circle),
                    contentDescription = "Check",
                    modifier = Modifier.padding(start = 8.dp),
                )
                Text(
                    text = stringResource(R.string.nc_transaction_enough_signers),
                    style = NunchukTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}