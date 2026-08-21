package com.nunchuk.android.signer.bitbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object BitBoxAppRequiredRoute

/**
 * Design screen 05B — the dead end for a device Nunchuk cannot use yet: uninitialized, in a
 * firmware-upgrade-only state, or sitting in its bootloader.
 *
 * This is where the whole BitBoxApp surface went. The first design carried device setup and
 * firmware upgrade inside Nunchuk; the 2026-08-21 revision cut all of it, so the app's job is
 * to name the problem and hand the user to BitBoxApp. "Scan again" returns to the device picker
 * so a device prepared in the meantime can be picked up without restarting the flow.
 */
fun NavGraphBuilder.bitBoxAppRequired(
    onBack: () -> Unit = {},
    onOpenBitBoxApp: () -> Unit = {},
    onScanAgain: () -> Unit = {},
) {
    composable<BitBoxAppRequiredRoute> {
        BitBoxAppRequiredScreen(
            onBack = onBack,
            onOpenBitBoxApp = onOpenBitBoxApp,
            onScanAgain = onScanAgain,
        )
    }
}

fun NavHostController.navigateToBitBoxAppRequired() {
    navigate(BitBoxAppRequiredRoute)
}

@Composable
private fun BitBoxAppRequiredScreen(
    onBack: () -> Unit = {},
    onOpenBitBoxApp: () -> Unit = {},
    onScanAgain: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_bitbox_device,
                onClosedClicked = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onOpenBitBoxApp,
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_open_bitbox_app))
                }
                NcOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onScanAgain,
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_scan_again))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.nc_bitbox_app_required_title),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(id = R.string.nc_bitbox_app_required_desc),
                style = NunchukTheme.typography.body,
            )
            BitBoxStep(index = 1) {
                Text(
                    text = stringResource(id = R.string.nc_bitbox_app_required_step_open),
                    style = NunchukTheme.typography.body,
                )
            }
            BitBoxStep(index = 2) {
                Text(
                    text = stringResource(id = R.string.nc_bitbox_app_required_step_finish),
                    style = NunchukTheme.typography.body,
                )
            }
            BitBoxStep(index = 3) {
                Text(
                    text = stringResource(id = R.string.nc_bitbox_app_required_step_return),
                    style = NunchukTheme.typography.body,
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BitBoxAppRequiredScreenPreview() {
    NunchukTheme { BitBoxAppRequiredScreen() }
}
