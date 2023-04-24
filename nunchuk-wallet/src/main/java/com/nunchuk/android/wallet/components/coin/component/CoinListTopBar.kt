package com.nunchuk.android.wallet.components.coin.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun CoinListTopBarNoneMode(
    enableSelectMode: () -> Unit,
    onShowMoreOptions: () -> Unit,
    title: String,
    isShowMore: Boolean,
) {
    NcTopAppBar(
        title = title,
        textStyle = NunchukTheme.typography.titleLarge,
        isBack = false,
        actions = {
            Text(
                modifier = Modifier.clickable { enableSelectMode() },
                text = stringResource(R.string.nc_select),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
            if (isShowMore) {
                IconButton(onClick = onShowMoreOptions) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more),
                        contentDescription = "More icon"
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        },
    )
}

@Composable
fun CoinListTopBarSelectMode(
    isSelectAll: Boolean,
    onSelectOrUnselectAll: (isSelect: Boolean) -> Unit = {},
    onSelectDone: () -> Unit
) {
    Surface(elevation = AppBarDefaults.TopAppBarElevation) {
        Row(
            modifier = Modifier
                .background(color = MaterialTheme.colors.background)
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .clickable { onSelectOrUnselectAll(isSelectAll.not()) },
                text = if (isSelectAll) stringResource(R.string.nc_unselect_all) else stringResource(
                    R.string.nc_select_all
                ),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.nc_select_coins), style = NunchukTheme.typography.titleLarge
            )
            Text(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clickable { onSelectDone() },
                text = stringResource(R.string.nc_text_done),
                style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
            )
        }
    }
}