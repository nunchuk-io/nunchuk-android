package com.nunchuk.android.wallet.components.coin.detail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.CoinTagView
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.wallet.R

@Composable
fun TagHorizontalList(
    modifier: Modifier = Modifier,
    output: UnspentOutput,
    onUpdateTag: (output: UnspentOutput) -> Unit,
    coinTags: Map<Int, CoinTag>,
    onViewTagDetail: (tag: CoinTag) -> Unit
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_coin_tag),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_tags),
            style = NunchukTheme.typography.title
        )
        if (output.tags.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = "(${output.tags.size})",
                style = NunchukTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (output.tags.isNotEmpty()) {
            Text(
                modifier = Modifier.clickable { onUpdateTag(output) },
                text = stringResource(id = R.string.nc_edit),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
            )
        } else {
            Icon(
                modifier = Modifier.clickable { onUpdateTag(output) },
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
    if (output.tags.isNotEmpty()) {
        val tags = output.tags.mapNotNull {
            coinTags[it]
        }.sortedBy { it.name }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                CoinTagView(
                    tag = tag,
                    circleSize = 24.dp,
                    textStyle = NunchukTheme.typography.body,
                    clickable = true
                ) {
                    onViewTagDetail(tag)
                }
            }
        }
    } else {
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.nc_coin_has_no_tags),
            textAlign = TextAlign.Center,
            style = NunchukTheme.typography.body
        )
    }
}

@Composable
fun TagHorizontalList(
    modifier: Modifier = Modifier,
    tags: List<CoinTag>,
    onViewAll: () -> Unit,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .size(24.dp),
            painter = painterResource(id = R.drawable.ic_coin_tag),
            contentDescription = "Lock icon"
        )
        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(R.string.nc_tags),
            style = NunchukTheme.typography.title
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.clickable { onViewAll() },
            text = stringResource(R.string.nc_view_all),
            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline),
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tags) { tag ->
            CoinTagView(
                tag = tag,
                circleSize = 24.dp,
                textStyle = NunchukTheme.typography.body,
                clickable = false
            )
        }
    }
}