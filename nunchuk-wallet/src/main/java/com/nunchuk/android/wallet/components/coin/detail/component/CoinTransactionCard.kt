package com.nunchuk.android.wallet.components.coin.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.TransactionNoteView
import com.nunchuk.android.core.util.formatAddress
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.model.Transaction

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
            TransactionNoteView(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                note = transaction.memo
            )
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