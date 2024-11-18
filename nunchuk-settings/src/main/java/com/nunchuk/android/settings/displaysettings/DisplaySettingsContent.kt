package com.nunchuk.android.settings.displaysettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.domain.data.SAT
import com.nunchuk.android.model.ThemeMode
import com.nunchuk.android.settings.R


@Composable
fun DisplaySettingsContent(
    uiState: DisplaySettingsUiState = DisplaySettingsUiState(),
    onDisplayUnitClick: () -> Unit = { },
    onWalletVisibilityClick: () -> Unit = { },
    onAppearanceClick: () -> Unit = { },
) {
    val themeModeLabel = when (uiState.themeMode) {
        ThemeMode.Light -> stringResource(id = R.string.nc_light)
        ThemeMode.Dark -> stringResource(id = R.string.nc_dark)
        ThemeMode.System -> stringResource(id = R.string.nc_automatic)
    }
    NunchukTheme {
        Scaffold(
            modifier = Modifier.statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(id = R.string.nc_display_settings),
                    textStyle = NunchukTheme.typography.titleLarge
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onDisplayUnitClick() }
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_display_unit),
                        style = NunchukTheme.typography.body
                    )
                    val unitText = if (uiState.unit == SAT) {
                        stringResource(R.string.nc_currency_sat)
                    } else {
                        stringResource(R.string.nc_currency_btc)
                    }
                    Text(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .weight(1f, true),
                        text = "($unitText)",
                        style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                    )

                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAppearanceClick() }
                        .padding(top = 12.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nc_appearance),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        modifier = Modifier.padding(end = 8.dp),
                        text = themeModeLabel,
                        style = NunchukTheme.typography.body.copy(color = MaterialTheme.colorScheme.textSecondary)
                    )

                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWalletVisibilityClick() }
                        .padding(top = 12.dp, bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.nc_wallet_visibility_settings),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.weight(1f)
                    )

                    NcIcon(
                        painter = painterResource(id = R.drawable.ic_arrow),
                        contentDescription = ""
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
fun DisplaySettingsContentPreview() {
    DisplaySettingsContent()
}