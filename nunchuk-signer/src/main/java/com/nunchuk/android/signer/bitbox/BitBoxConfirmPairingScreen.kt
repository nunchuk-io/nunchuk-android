package com.nunchuk.android.signer.bitbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object BitBoxConfirmPairingRoute

/**
 * Design screen 05 — the pairing code shown on the phone, to be compared with the one on the
 * device.
 *
 * Only reached for a **new** pairing: pairing data is stored per Nunchuk account/chain, so a
 * device paired before goes straight through. Both answers are terminal for the pairing step —
 * "Codes don't match" tells the native session the user rejected it, which fails the operation
 * with `PAIRING_REJECTED`.
 */
fun NavGraphBuilder.bitBoxConfirmPairing(
    pairingCode: () -> String = { "" },
    onBack: () -> Unit = {},
    onConfirm: (accepted: Boolean) -> Unit = {},
) {
    composable<BitBoxConfirmPairingRoute> {
        BitBoxConfirmPairingScreen(
            pairingCode = pairingCode(),
            onBack = onBack,
            onConfirm = onConfirm,
        )
    }
}

fun NavHostController.navigateToBitBoxConfirmPairing() {
    navigate(BitBoxConfirmPairingRoute)
}

@Composable
private fun BitBoxConfirmPairingScreen(
    pairingCode: String = "",
    onBack: () -> Unit = {},
    onConfirm: (accepted: Boolean) -> Unit = {},
) {
    Scaffold(
        topBar = { NcTopAppBar(title = "", onBackPress = onBack) },
        bottomBar = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onConfirm(true) },
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_codes_match))
                }
                NcOutlineButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onConfirm(false) },
                ) {
                    Text(text = stringResource(id = R.string.nc_bitbox_codes_do_not_match))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.nc_bitbox_confirm_pairing),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(id = R.string.nc_bitbox_confirm_pairing_desc),
                style = NunchukTheme.typography.body,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NcIcon(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .size(24.dp),
                        painter = painterResource(id = R.drawable.ic_bitbox_hardware),
                        contentDescription = null,
                    )
                    // Monospaced + wide tracking so the user can compare it character by
                    // character against the device screen without miscounting.
                    Text(
                        text = pairingCode,
                        style = NunchukTheme.typography.heading.copy(
                            fontSize = 28.sp,
                            letterSpacing = 2.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // The device is asking the user the same question at the same time; the spinner
            // says Nunchuk is still waiting on that answer, which is what resolves the step
            // over BLE (there, the app side needs no button at all).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.textSecondary,
                )
                Text(
                    modifier = Modifier.padding(start = 12.dp),
                    text = stringResource(id = R.string.nc_bitbox_confirm_pairing_hint),
                    style = NunchukTheme.typography.bodySmall
                        .copy(color = MaterialTheme.colorScheme.textSecondary),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BitBoxConfirmPairingScreenPreview() {
    NunchukTheme { BitBoxConfirmPairingScreen(pairingCode = "VQ237 XAFZK") }
}
