package com.nunchuk.android.settings.displaysettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcRadioButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.lightGray
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.model.ThemeMode
import com.nunchuk.android.settings.R

@Composable
fun AppearanceScreen(
    uiState: DisplaySettingsUiState = DisplaySettingsUiState(),
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_appearance),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.nc_automatic),
                            style = NunchukTheme.typography.body
                        )
                        Text(
                            text = stringResource(id = R.string.nc_appearance_system_description),
                            style = NunchukTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textSecondary
                        )
                    }

                    NcSwitch(
                        checked = uiState.themeMode == ThemeMode.System,
                        onCheckedChange = { onThemeModeChange(ThemeMode.System) },
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ThemeCard(
                        modifier = Modifier.weight(1f),
                        description = stringResource(id = R.string.nc_light),
                        icon = R.drawable.ic_theme_sun,
                        checked = uiState.themeMode == ThemeMode.Light,
                        onClicked = { onThemeModeChange(ThemeMode.Light) }
                    )

                    ThemeCard(
                        modifier = Modifier.weight(1f),
                        description = stringResource(id = R.string.nc_dark),
                        checked = uiState.themeMode == ThemeMode.Dark,
                        icon = R.drawable.ic_theme_dark,
                        onClicked = { onThemeModeChange(ThemeMode.Dark) }
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeCard(
    description: String,
    checked: Boolean,
    icon: Int,
    modifier: Modifier = Modifier,
    onClicked: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClicked)
            .background(
                color = if (checked) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.lightGray,
                shape = NunchukTheme.shape.medium
            )
            .then(
                if (checked) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.textPrimary,
                    shape = NunchukTheme.shape.medium
                ) else Modifier
            ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            NcIcon(
                painter = painterResource(id = icon),
                contentDescription = ""
            )
            Text(
                text = description,
                style = NunchukTheme.typography.body,
            )
        }

        NcRadioButton(
            selected = checked,
            onClick = onClicked,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@PreviewLightDark
@Composable
fun AppearanceScreenPreview() {
    AppearanceScreen()
}