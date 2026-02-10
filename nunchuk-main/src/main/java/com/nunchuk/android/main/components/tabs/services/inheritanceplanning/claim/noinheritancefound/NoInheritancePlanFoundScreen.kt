package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.noinheritancefound

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.widget.R as WidgetR

@Composable
fun InheritanceErrorScreen(
    snackState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit = {},
    title: String,
    customMessage: String,
) {
    InheritanceErrorContent(
        modifier = modifier,
        snackState = snackState,
        onCloseClick = onCloseClick,
        displayTitle = title,
        description = customMessage,
    )
}

@Composable
private fun InheritanceErrorContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onCloseClick: () -> Unit = {},
    displayTitle: String,
    description: String,
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(
                title = "",
                isBack = false
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onCloseClick,
            ) {
                Text(text = stringResource(id = WidgetR.string.nc_text_done))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            NcCircleImage(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(80.dp)
                    .align(Alignment.CenterHorizontally),
                iconSize = 48.dp,
                size = 96.dp,
                color = colorResource(id = WidgetR.color.nc_red_tint_color),
                iconTintColor = colorResource(id = WidgetR.color.nc_orange_dark_color),
                resId = WidgetR.drawable.ic_info
            )
            Spacer(modifier = Modifier.padding(top = 24.dp))
            Text(
                text = displayTitle,
                style = NunchukTheme.typography.heading,
            )
            Spacer(modifier = Modifier.padding(top = 16.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun InheritanceErrorScreenPreview() {
    NunchukTheme {
        InheritanceErrorContent(
            onCloseClick = {},
            displayTitle = stringResource(id = MainR.string.nc_no_inheritance_plan_found),
            description = stringResource(id = MainR.string.nc_no_inheritance_plan_found_desc)
        )
    }
}

