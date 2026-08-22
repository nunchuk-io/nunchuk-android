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
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.bitbox.BitBoxPairingCodeBody
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
        BitBoxPairingCodeBody(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            pairingCode = pairingCode,
        )
    }
}

@PreviewLightDark
@Composable
private fun BitBoxConfirmPairingScreenPreview() {
    NunchukTheme { BitBoxConfirmPairingScreen(pairingCode = "VQ237 XAFZK") }
}
