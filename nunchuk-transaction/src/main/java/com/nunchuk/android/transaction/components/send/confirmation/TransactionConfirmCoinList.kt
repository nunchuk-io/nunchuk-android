package com.nunchuk.android.transaction.components.send.confirmation

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.nunchuk.android.compose.MODE_VIEW_ONLY
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput

@Composable
fun TransactionConfirmCoinList(inputs: List<UnspentOutput>, allTags: Map<Int, CoinTag>) {
    NunchukTheme {
        Column {
            inputs.forEach {
                PreviewCoinCard(
                    output = it,
                    mode = MODE_VIEW_ONLY,
                    tags = allTags
                )
            }
        }
    }
}