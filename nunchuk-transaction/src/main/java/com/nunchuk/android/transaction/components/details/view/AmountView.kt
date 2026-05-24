package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.formatDecimalWithoutZero
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.model.Amount

private const val LIQUID_FRACTION_DIGITS = 8

@Composable
fun AmountView(
    amount: Amount,
    hideFiatCurrency: Boolean = false,
    assetId: String = "",
    usdtAssetId: String = "",
) {
    if (assetId.isNotEmpty()) {
        AssetAmountText(amount = amount, assetId = assetId, usdtAssetId = usdtAssetId)
        return
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End,
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

@Composable
private fun AssetAmountText(amount: Amount, assetId: String, usdtAssetId: String) {
    val symbol = if (assetId == usdtAssetId) "USDT" else "LBTC"
    val value = amount.pureBTC().formatDecimalWithoutZero(maxFractionDigits = LIQUID_FRACTION_DIGITS)
    Text(
        text = "$value $symbol",
        style = NunchukTheme.typography.title,
    )
}