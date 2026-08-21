package com.nunchuk.android.signer.bitbox

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
import kotlinx.serialization.Serializable

@Serializable
internal data object BitBoxIntroRoute

/**
 * Design screens 01A / 01B — the transport choice that opens the flow. Bluetooth is Nova-only;
 * USB covers every BitBox02 model, so each row carries the models it applies to as a subtitle.
 */
fun NavGraphBuilder.bitBoxIntro(
    onBack: () -> Unit = {},
    onAddViaBluetooth: () -> Unit = {},
    onAddViaUsb: () -> Unit = {},
    onAddViaDesktop: () -> Unit = {},
    isAddViaDesktopEnabled: Boolean = false,
) {
    composable<BitBoxIntroRoute> {
        BitBoxIntroScreen(
            onBack = onBack,
            onAddViaBluetooth = onAddViaBluetooth,
            onAddViaUsb = onAddViaUsb,
            onAddViaDesktop = onAddViaDesktop,
            isAddViaDesktopEnabled = isAddViaDesktopEnabled,
        )
    }
}

@Composable
fun BitBoxIntroScreen(
    onBack: () -> Unit = {},
    onAddViaBluetooth: () -> Unit = {},
    onAddViaUsb: () -> Unit = {},
    onAddViaDesktop: () -> Unit = {},
    isAddViaDesktopEnabled: Boolean = false,
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_bitbox_illustration,
                onClosedClicked = onBack,
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
                text = stringResource(id = R.string.nc_add_bitbox),
                style = NunchukTheme.typography.heading,
            )

            BitBoxActionItem(
                modifier = Modifier.padding(top = 24.dp),
                iconRes = R.drawable.ic_bluetooth,
                title = stringResource(id = R.string.nc_add_bitbox_via_bluetooth),
                subtitle = stringResource(id = R.string.nc_add_bitbox_via_bluetooth_desc),
                isEnabled = true,
                onClick = onAddViaBluetooth,
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
            )

            BitBoxActionItem(
                iconRes = R.drawable.ic_usb,
                title = stringResource(id = R.string.nc_add_bitbox_via_usb),
                subtitle = stringResource(id = R.string.nc_add_bitbox_via_usb_desc),
                isEnabled = true,
                onClick = onAddViaUsb,
            )

            // Assisted/group membership flows keep the desktop app as an escape hatch, so a user
            // whose device won't pair over BLE/USB can still claim the key from desktop.
            if (isAddViaDesktopEnabled) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                )

                BitBoxActionItem(
                    iconRes = R.drawable.ic_desktop,
                    title = stringResource(id = R.string.nc_add_bitbox_via_desktop),
                    subtitle = null,
                    isEnabled = true,
                    onClick = onAddViaDesktop,
                )
            }
        }
    }
}

@Composable
private fun BitBoxActionItem(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String?,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(if (isEnabled) 1f else 0.6f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NcIcon(
            painter = painterResource(id = iconRes),
            contentDescription = "",
            modifier = Modifier.size(24.dp),
        )

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = title, style = NunchukTheme.typography.body)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = NunchukTheme.typography.bodySmall
                        .copy(color = MaterialTheme.colorScheme.textSecondary),
                )
            }
        }

        if (isEnabled) {
            NcIcon(painter = painterResource(id = R.drawable.ic_arrow), contentDescription = "")
        }
    }
}

@PreviewLightDark
@Composable
private fun BitBoxIntroScreenPreview() {
    NunchukTheme {
        BitBoxIntroScreen()
    }
}

@PreviewLightDark
@Composable
private fun BitBoxIntroScreenWithDesktopPreview() {
    NunchukTheme {
        BitBoxIntroScreen(isAddViaDesktopEnabled = true)
    }
}
