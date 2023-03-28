package com.nunchuk.android.wallet.components.coin.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.CoinTagAddition
import com.nunchuk.android.wallet.R
import com.nunchuk.android.wallet.components.coin.tag.TagFlow
import com.nunchuk.android.wallet.components.coin.tag.TagItem

@Composable
fun SelectTagContent(
    tags: List<CoinTagAddition> = emptyList(),
    selectedCoinTags: Set<Int> = emptySet(),
    onBackPressed: () -> Unit = {},
    onCheckedChange: ((Int, Boolean) -> Unit) = { _, _ -> },
    onSelectDone: () -> Unit = {},
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
) {
    val isSelectAll = selectedCoinTags.size == tags.size
    NunchukTheme {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .background(
                    color = MaterialTheme.colors.surface,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                IconButton(modifier = Modifier.align(Alignment.CenterStart), onClick = {
                    onBackPressed()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back icon"
                    )
                }
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.nc_select_coins),
                    style = NunchukTheme.typography.titleLarge
                )
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .clickable { onSelectOrUnselectAll(isSelectAll.not()) },
                    text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                        R.string.nc_select_all
                    ),
                    style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1.0f), verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tags) { tag ->
                    TagItem(id = tag.coinTag.id,
                        name = tag.coinTag.name,
                        color = tag.coinTag.color,
                        numCoins = tag.numCoins,
                        checked = selectedCoinTags.contains(tag.coinTag.id),
                        onTagClick = { },
                        tagFlow = TagFlow.ADD,
                        onCheckedChange = {
                            onCheckedChange(tag.coinTag.id, it)
                        })
                }
            }

            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = onSelectDone,
            ) {
                Text(text = stringResource(R.string.nc_apply))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectTagContentPreview() {
    SelectTagContent()
}
