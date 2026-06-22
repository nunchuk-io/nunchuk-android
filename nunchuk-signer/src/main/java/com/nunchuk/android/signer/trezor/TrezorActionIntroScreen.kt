package com.nunchuk.android.signer.trezor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.signer.R

const val trezorActionIntroRoute = "trezor_action_intro_route"

fun NavGraphBuilder.trezorActionIntro(
    onBack: () -> Unit = {},
    onAddViaSuite: () -> Unit = {},
    onAddViaUsb: () -> Unit = {},
    isAddViaUsbEnabled: Boolean = false
) {
    composable(trezorActionIntroRoute) {
        TrezorActionIntroScreen(
            onBack = onBack,
            onAddViaSuite = onAddViaSuite,
            onAddViaUsb = onAddViaUsb,
            isAddViaUsbEnabled = isAddViaUsbEnabled
        )
    }
}

@Composable
fun TrezorActionIntroScreen(
    onBack: () -> Unit = {},
    onAddViaSuite: () -> Unit = {},
    onAddViaUsb: () -> Unit = {},
    isAddViaUsbEnabled: Boolean = false
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_trezor_illustration,
                onClosedClicked = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(id = R.string.nc_add_trezor),
                style = NunchukTheme.typography.heading
            )

            TrezorActionItem(
                modifier = Modifier.padding(top = 24.dp),
                iconRes = R.drawable.ic_trezor_hardware,
                title = stringResource(id = R.string.nc_add_trezor_via_suite),
                subtitle = stringResource(id = R.string.nc_bluetooth_or_cable),
                isEnabled = true,
                onClick = onAddViaSuite
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp
            )

            TrezorActionItem(
                iconRes = R.drawable.ic_usb,
                title = stringResource(id = R.string.nc_add_trezor_via_usb),
                subtitle = stringResource(id = R.string.nc_desktop_only),
                isEnabled = isAddViaUsbEnabled,
                onClick = onAddViaUsb
            )
        }
    }
}

@Composable
private fun TrezorActionItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (isEnabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NcIcon(
            painter = painterResource(id = iconRes),
            contentDescription = "",
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body
            )
            Text(
                text = subtitle,
                style = NunchukTheme.typography.bodySmall
                    .copy(color = MaterialTheme.colorScheme.textSecondary)
            )
        }

        if (isEnabled) {
            NcIcon(
                painter = painterResource(id = R.drawable.ic_arrow),
                contentDescription = ""
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TrezorActionIntroScreenPreview() {
    NunchukTheme {
        TrezorActionIntroScreen()
    }
}
