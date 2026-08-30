package com.nunchuk.android.signer.bitbox

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data class BitBoxInstructionRoute(val isUsb: Boolean = false)

/**
 * Design screens 02A / 02B — "prepare your BitBox", one variant per transport. It always comes
 * before the device picker: settled on the thread that the intro screen leads in every flow
 * (Trezor Android already does this; Ledger shows its instructions for Bluetooth only).
 *
 * Step 2 and the illustration are the only difference between the two variants — enable
 * Bluetooth, or plug the device straight into the phone's USB-C port. Step 3 only appears where
 * a select-wallet-type screen actually follows: assisted wallets have that config fixed for
 * them, so the screen — and this step with it — drops out.
 */
fun NavGraphBuilder.bitBoxInstruction(
    onBack: () -> Unit = {},
    onContinue: (isUsb: Boolean) -> Unit = {},
    hasSelectWalletTypeStep: Boolean = true,
) {
    composable<BitBoxInstructionRoute> { entry ->
        val route = entry.toRoute<BitBoxInstructionRoute>()
        BitBoxInstructionScreen(
            isUsb = route.isUsb,
            hasSelectWalletTypeStep = hasSelectWalletTypeStep,
            onBack = onBack,
            onContinue = { onContinue(route.isUsb) },
        )
    }
}

fun NavHostController.navigateToBitBoxInstruction(isUsb: Boolean) {
    navigate(BitBoxInstructionRoute(isUsb = isUsb))
}

@Composable
private fun BitBoxInstructionScreen(
    isUsb: Boolean = false,
    hasSelectWalletTypeStep: Boolean = true,
    onBack: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                // One illustration per transport, as in the design: over the air for
                // Bluetooth, plugged straight into the phone for USB.
                backgroundRes = if (isUsb) {
                    R.drawable.bg_bitbox_instructions_usb
                } else {
                    R.drawable.bg_bitbox_instructions_bluetooth
                },
                onClosedClicked = onBack,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                onClick = onContinue,
            ) {
                Text(text = stringResource(id = com.nunchuk.android.core.R.string.nc_text_continue))
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
                text = stringResource(id = R.string.nc_add_your_bitbox),
                style = NunchukTheme.typography.heading,
            )

            Text(
                text = stringResource(id = R.string.nc_bitbox_before_continuing),
                style = NunchukTheme.typography.body,
            )

            // Setup is not something Nunchuk drives — an unprepared device is sent to BitBoxApp
            // (see BitBoxAppRequiredScreen), so say so up front rather than after pairing fails.
            BitBoxStep(index = 1) {
                Text(
                    text = stringResource(id = R.string.nc_bitbox_step_setup_title),
                    style = NunchukTheme.typography.title,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(id = R.string.nc_bitbox_step_setup_desc),
                    style = NunchukTheme.typography.body,
                )
            }

            BitBoxStep(index = 2) {
                Text(
                    text = stringResource(
                        id = if (isUsb) {
                            R.string.nc_bitbox_step_connect_usb_title
                        } else {
                            R.string.nc_bitbox_step_connect_bluetooth_title
                        }
                    ),
                    style = NunchukTheme.typography.title,
                )
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = stringResource(
                        id = if (isUsb) {
                            R.string.nc_bitbox_step_connect_usb_desc
                        } else {
                            R.string.nc_bitbox_step_connect_bluetooth_desc
                        }
                    ),
                    style = NunchukTheme.typography.body,
                )
            }

            if (hasSelectWalletTypeStep) {
                BitBoxStep(index = 3) {
                    Text(
                        text = stringResource(id = R.string.nc_bitbox_step_return_title),
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(id = R.string.nc_bitbox_step_return_desc),
                        style = NunchukTheme.typography.body,
                    )
                }
            }
        }
    }
}

@Composable
internal fun BitBoxStep(
    index: Int,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                style = NunchukTheme.typography.titleSmall.copy(fontWeight = FontWeight.W700),
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
        ) {
            content()
        }
    }
}

@PreviewLightDark
@Composable
private fun BitBoxInstructionBluetoothPreview() {
    NunchukTheme { BitBoxInstructionScreen(isUsb = false) }
}

@PreviewLightDark
@Composable
private fun BitBoxInstructionUsbPreview() {
    NunchukTheme { BitBoxInstructionScreen(isUsb = true) }
}

@PreviewLightDark
@Composable
private fun BitBoxInstructionAssistedPreview() {
    NunchukTheme { BitBoxInstructionScreen(isUsb = false, hasSelectWalletTypeStep = false) }
}
