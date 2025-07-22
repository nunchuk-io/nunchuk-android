package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.transaction.R

@Composable
fun TransactionOutputItem(
    savedAddresses: Map<String, String>,
    output: TxOutput,
    onCopyText: (String) -> Unit,
    hideFiatCurrency: Boolean = false
) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        if (savedAddresses.contains(output.first)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_saved_address),
                    contentDescription = "Saved Address",
                    modifier = Modifier.size(16.dp),
                )

                Text(
                    text = savedAddresses[output.first].orEmpty(),
                    style = NunchukTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TransactionOutputItem(
            output = output,
            onCopyText = onCopyText,
            hideFiatCurrency = hideFiatCurrency
        )
    }
}

@Composable
fun TransactionOutputItem(
    output: TxOutput,
    onCopyText: (String) -> Unit,
    hideFiatCurrency: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = { onCopyText(output.first) }),
            text = output.first,
            style = NunchukTheme.typography.title,
        )

        AmountView(output.second, hideFiatCurrency)
    }
}