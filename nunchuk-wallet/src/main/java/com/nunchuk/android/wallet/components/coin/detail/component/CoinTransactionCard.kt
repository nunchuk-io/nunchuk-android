package com.nunchuk.android.wallet.components.coin.detail.component

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NcLinkifyText
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.formatAddress
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Transaction
import com.nunchuk.android.wallet.R

@Composable
fun CoinTransactionCard(
    transaction: Transaction,
    onNoteClick: (note: String) -> Unit = {},
    onViewTransactionDetail: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .padding(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
            )
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colors.background, shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp, color = NcColor.border, shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = true, onClick = onViewTransactionDetail)
            .padding(16.dp)
    ) {
        Text(
            text = transaction.formatAddress(LocalContext.current),
            style = NunchukTheme.typography.bodySmall,
        )
        Text(
            text = transaction.totalAmount.getBTCAmount(),
            style = NunchukTheme.typography.body,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (transaction.memo.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .clickable(enabled = Patterns.WEB_URL.matcher(transaction.memo).find()) { onNoteClick(transaction.memo) }
                    .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier
                        .border(1.dp, color = NcColor.border, shape = CircleShape)
                        .padding(4.dp),
                    painter = painterResource(id = R.drawable.ic_transaction_note),
                    contentDescription = "Transaction Note"
                )
                NcLinkifyText(
                    modifier = Modifier.padding(start = 4.dp),
                    text = transaction.memo,
                    style = NunchukTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview
@Composable
fun CoinTransactionCardPreview() {
    NunchukTheme {
        CoinTransactionCard(transaction = Transaction(memo = "Hello My name is Hai"))
    }
}