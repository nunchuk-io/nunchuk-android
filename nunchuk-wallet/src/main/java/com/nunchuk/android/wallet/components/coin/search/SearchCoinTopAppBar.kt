package com.nunchuk.android.wallet.components.coin.search

import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcColor
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.wallet.R

@Composable
fun SearchCoinTopAppBar(
    modifier: Modifier = Modifier,
    onBackPressOwner: OnBackPressedDispatcherOwner?,
    query: String,
    isEmpty: Boolean = false,
    onQueryChange: (String) -> Unit,
    enableSelectMode: () -> Unit,
    onFilterClicked: () -> Unit,
    isShowSelect: Boolean = true
) {
    val isShowClearSearch by remember(query) {
        derivedStateOf { query.isNotEmpty() }
    }
    TopAppBar(
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            if (isShowSelect) {
                AnimatedVisibility(visible = isEmpty.not()) {
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable { enableSelectMode() },
                        text = stringResource(R.string.nc_select),
                        style = NunchukTheme.typography.title.copy(textDecoration = TextDecoration.Underline)
                    )
                }
            }
            IconButton(onClick = onFilterClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter),
                    contentDescription = "Filter icon"
                )
            }
        },
        title = {
            TextField(
                modifier = Modifier
                    .background(color = MaterialTheme.colors.background)
                    .border(
                        color = NcColor.border,
                        width = 1.dp,
                        shape = RoundedCornerShape(44.dp),
                    ),
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
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                trailingIcon = {
                    if (isShowClearSearch) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close icon"
                            )
                        }
                    }
                }
            )
        }, elevation = 0.dp,
        backgroundColor = MaterialTheme.colors.background
    )
}