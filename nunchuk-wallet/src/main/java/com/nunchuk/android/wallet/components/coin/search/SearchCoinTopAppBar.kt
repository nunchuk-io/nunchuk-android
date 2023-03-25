package com.nunchuk.android.wallet.components.coin.search

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun SearchCoinTopAppBar(
    onBackPressOwner: OnBackPressedDispatcherOwner?,
    query: String,
    onQueryChange: (String) -> Unit,
    enableSelectMode: () -> Unit,
    onFilterClicked: () -> Unit
) {
    TopAppBar(navigationIcon = {
        IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back"
            )
        }
    }, actions = {
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_close),
                    contentDescription = "Close icon"
                )
            }
        }
        Text(
            modifier = Modifier.clickable { enableSelectMode() },
            text = stringResource(R.string.nc_select),
            style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
        )
        IconButton(onClick = onFilterClicked) {
            Icon(
                painter = painterResource(id = R.drawable.ic_filter),
                contentDescription = "Filter icon"
            )
        }
    }, title = {
        TextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
            },
            textStyle = NunchukTheme.typography.body,
            placeholder = {
                Text(text = stringResource(R.string.nc_search_coins))
            },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
        )
    }, elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.background
    )
}