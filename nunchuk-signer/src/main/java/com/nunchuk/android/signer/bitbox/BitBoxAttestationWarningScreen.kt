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
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R
import kotlinx.serialization.Serializable

@Serializable
internal data object BitBoxAttestationWarningRoute

/**
 * Design screen 05C — attestation failed, so Nunchuk could not establish that this is a genuine
 * BitBox.
 *
 * Deliberately a dead end with one way out: per the Confluence guidance an INVALID attestation
 * means the device must not be used to add a key or approve a transaction, so there is no
 * "continue anyway".
 */
fun NavGraphBuilder.bitBoxAttestationWarning(
    onDisconnect: () -> Unit = {},
) {
    composable<BitBoxAttestationWarningRoute> {
        BitBoxAttestationWarningScreen(onDisconnect = onDisconnect)
    }
}

fun NavHostController.navigateToBitBoxAttestationWarning() {
    navigate(BitBoxAttestationWarningRoute)
}

@Composable
private fun BitBoxAttestationWarningScreen(
    onDisconnect: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_bitbox_attestation_warning,
                onClosedClicked = onDisconnect,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .fillMaxWidth(),
                onClick = onDisconnect,
            ) {
                Text(text = stringResource(id = R.string.nc_bitbox_disconnect))
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
                text = stringResource(id = R.string.nc_bitbox_attestation_title),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(id = R.string.nc_bitbox_attestation_desc),
                style = NunchukTheme.typography.body,
            )
            NcHintMessage(
                modifier = Modifier.fillMaxWidth(),
                type = HighlightMessageType.WARNING,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.nc_bitbox_attestation_warning_title),
                        style = NunchukTheme.typography.title,
                    )
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = stringResource(id = R.string.nc_bitbox_attestation_warning_desc),
                        style = NunchukTheme.typography.body,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BitBoxAttestationWarningScreenPreview() {
    NunchukTheme { BitBoxAttestationWarningScreen() }
}
