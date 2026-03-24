package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.CoinTagGroupView
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.model.UnspentOutput

@Composable
fun ChangeAddressView(
    modifier: Modifier = Modifier,
    txOutput: TxOutput,
    output: UnspentOutput?,
    tags: Map<Int, CoinTag>,
    onCopyText: (String) -> Unit,
    onInspectAddress: (String) -> Unit,
    hideFiatCurrency: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AddressWithInspect(
                modifier = Modifier
                    .weight(1f),
                address = txOutput.first,
                onCopyText = onCopyText,
                onInspectAddress = onInspectAddress,
            )

            AmountView(txOutput.second, hideFiatCurrency)
        }

        if (output != null && output.tags.isNotEmpty()) {
            CoinTagGroupView(
                modifier = Modifier.padding(top = 8.dp),
                tagIds = output.tags, tags = tags
            )
        }
    }
}