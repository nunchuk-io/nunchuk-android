package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.model.TxOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.core.R as CoreR

private const val INSPECT_INLINE_ID = "inspect"

@Composable
fun TransactionOutputItem(
    savedAddresses: Map<String, String>,
    output: TxOutput,
    onCopyText: (String) -> Unit,
    onInspectAddress: (String) -> Unit,
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
            onInspectAddress = onInspectAddress,
            hideFiatCurrency = hideFiatCurrency
        )
    }
}

@Composable
fun TransactionOutputItem(
    output: TxOutput,
    onCopyText: (String) -> Unit,
    onInspectAddress: (String) -> Unit,
    hideFiatCurrency: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AddressWithInspect(
            modifier = Modifier.weight(1f),
            address = output.first,
            onCopyText = onCopyText,
            onInspectAddress = onInspectAddress,
        )

        AmountView(output.second, hideFiatCurrency)
    }
}

@Composable
fun AddressWithInspect(
    modifier: Modifier = Modifier,
    address: String,
    onCopyText: (String) -> Unit,
    onInspectAddress: (String) -> Unit,
) {
    val inspectLabel = stringResource(CoreR.string.nc_inspect)
    val annotatedString = buildAnnotatedString {
        append(address)
        append("  ")
        appendInlineContent(INSPECT_INLINE_ID, inspectLabel)
    }

    val inlineContent = mapOf(
        INSPECT_INLINE_ID to InlineTextContent(
            placeholder = Placeholder(
                width = 70.sp,
                height = 22.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            Text(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.controlFillPrimary,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onInspectAddress(address) }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                text = inspectLabel,
                style = NunchukTheme.typography.captionTitle,
            )
        }
    )

    Text(
        modifier = modifier.clickable { onCopyText(address) },
        text = annotatedString,
        style = NunchukTheme.typography.title,
        inlineContent = inlineContent,
    )
}
