package com.nunchuk.android.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R
import com.nunchuk.android.model.CoinTag

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoinTagGroupView(
    modifier: Modifier = Modifier,
    note: String = "",
    tagIds: Set<Int>,
    tags: Map<Int, CoinTag>
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, NcColor.border, RoundedCornerShape(12.dp))
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            maxItemsInEachRow = 4
        ) {
            tagIds.take(5).forEach {
                tags[it]?.let { coinTag ->
                    CoinTagView(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        tag = coinTag
                    )
                }
            }
            if (tagIds.size > 5) {
                Text(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    text = "+${tagIds.size - 5}",
                    style = NunchukTheme.typography.body,
                )
            }
        }
        Row(
            modifier = Modifier.padding(bottom = if (note.isNotEmpty()) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.isNotEmpty()) {
                Icon(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .border(1.dp, color = NcColor.border, shape = CircleShape)
                        .padding(4.dp),
                    painter = painterResource(id = R.drawable.ic_transaction_note),
                    contentDescription = "Transaction Note"
                )
                Text(
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                    text = note,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupViewPreview() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tagIds = setOf(1, 2, 3, 4, 5, 6, 7),
            tags = mapOf(
                1 to CoinTag(id = 1, name = "badcoins", color = "#000000"),
                2 to CoinTag(id = 2, name = "badcoins", color = "#000000"),
                3 to CoinTag(id = 3, name = "badcoins", color = "#000000"),
                4 to CoinTag(id = 4, name = "badcoins", color = "#000000"),
                5 to CoinTag(id = 5, name = "badcoins", color = "#000000"),
                6 to CoinTag(id = 6, name = "badcoins", color = "#000000"),
                7 to CoinTag(id = 7, name = "badcoins", color = "#000000"),
            ),
            note = "Send to Bob on Silk Road"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CoinTagGroupViewPreviewNoNote() {
    NunchukTheme {
        CoinTagGroupView(
            modifier = Modifier.padding(16.dp),
            tagIds = setOf(1, 2, 3, 4, 5, 6, 7),
            tags = mapOf(
                1 to CoinTag(id = 1, name = "badcoins", color = "#DDFFFF"),
                2 to CoinTag(id = 2, name = "badcoins", color = "#000000"),
                3 to CoinTag(id = 3, name = "badcoins", color = "#000000"),
                4 to CoinTag(id = 4, name = "badcoins", color = "#000000"),
                5 to CoinTag(id = 5, name = "badcoins", color = "#000000"),
                6 to CoinTag(id = 6, name = "badcoins", color = "#000000"),
                7 to CoinTag(id = 7, name = "badcoins", color = "#000000"),
            ),
            note = "",
        )
    }
}