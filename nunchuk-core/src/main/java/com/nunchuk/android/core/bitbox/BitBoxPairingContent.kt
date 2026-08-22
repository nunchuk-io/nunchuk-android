package com.nunchuk.android.core.bitbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textSecondary
import com.nunchuk.android.core.R

/**
 * The "confirm pairing" body — heading, the code to compare with the device, and the line
 * saying Nunchuk is still waiting on the device side.
 *
 * Shared so the full-screen add-key step (design screen 05) and the sign / health-check sheet
 * show the user the same thing. Only the answer buttons differ, and they belong to the host:
 * the screen puts them in its bottom bar, the sheet inline underneath.
 */
@Composable
fun BitBoxPairingCodeBody(
    modifier: Modifier = Modifier,
    pairingCode: String,
) {
    Column(
        modifier = modifier,
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
                // Wide tracking so the user can compare it character by character against the
                // device screen without miscounting.
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
        // The device is asking the user the same question at the same time; the spinner says
        // Nunchuk is still waiting on that answer, which is what resolves the step over BLE
        // (there, the app side needs no button at all).
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

@PreviewLightDark
@Composable
private fun BitBoxPairingCodeBodyPreview() {
    NunchukTheme { BitBoxPairingCodeBody(pairingCode = "VQ237 XAFZK") }
}
