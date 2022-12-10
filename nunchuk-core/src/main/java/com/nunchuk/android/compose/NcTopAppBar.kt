package com.nunchuk.android.compose

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

@Composable
fun NcTopAppBar(
    title: String,
    hasAction: Boolean = false,
    textStyle: TextStyle = NunchukTheme.typography.titleSmall,
    actions: @Composable RowScope.() -> Unit = {},
    isBack: Boolean = true,
) {
    val onBackPressOwner = LocalOnBackPressedDispatcherOwner.current
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 0.dp,
        navigationIcon = {
            IconButton(onClick = { onBackPressOwner?.onBackPressedDispatcher?.onBackPressed() }) {
                Icon(
                    painter = painterResource(id = if (isBack) R.drawable.ic_back else R.drawable.ic_close),
                    contentDescription = "Back"
                )
            }
        },
        title = {
            Text(
                text = title,
                style = textStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(end = if (hasAction) 0.dp else LocalViewConfiguration.current.minimumTouchTargetSize.width)
                    .fillMaxWidth(),
            )
        },
        actions = actions
    )
}

@Preview
@Composable
fun NcTopAppBarPreview() {
    NcTopAppBar(
        hasAction = true,
        title = "Est. time remaining: xx minutes",
    )
}