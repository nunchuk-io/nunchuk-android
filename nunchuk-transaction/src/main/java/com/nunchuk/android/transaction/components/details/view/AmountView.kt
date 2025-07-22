package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.model.Amount

@Composable
fun AmountView(amount: Amount, hideFiatCurrency: Boolean = false) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = amount.getBTCAmount(),
            style = NunchukTheme.typography.title,
        )

        if (!hideFiatCurrency) {
            Text(
                text = amount.getCurrencyAmount(),
                style = NunchukTheme.typography.bodySmall,
            )
        }
    }
}