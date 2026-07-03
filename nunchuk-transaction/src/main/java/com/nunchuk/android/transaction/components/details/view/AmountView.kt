package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.getLbtcAmount
import com.nunchuk.android.core.util.getLiquidCurrencyAmount
import com.nunchuk.android.core.util.getUsdtTokenAmount
import com.nunchuk.android.model.Amount

@Composable
fun AmountView(
    amount: Amount,
    hideFiatCurrency: Boolean = false,
    assetId: String = "",
    usdtAssetId: String = "",
) {
    if (assetId.isNotEmpty()) {
        AssetAmountText(
            amount = amount,
            assetId = assetId,
            usdtAssetId = usdtAssetId,
            hideFiatCurrency = hideFiatCurrency,
        )
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
private fun AssetAmountText(
    amount: Amount,
    assetId: String,
    usdtAssetId: String,
    hideFiatCurrency: Boolean,
) {
    // USDT keeps its fixed 8-decimal display; LBTC honours the selected unit setting.
    val text = if (assetId == usdtAssetId) {
        amount.getUsdtTokenAmount()
    } else {
        amount.getLbtcAmount()
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            text = text,
            style = NunchukTheme.typography.title,
        )
        if (!hideFiatCurrency) {
            Text(
                text = amount.getLiquidCurrencyAmount(assetId, usdtAssetId),
                style = NunchukTheme.typography.bodySmall,
            )
        }
    }
}