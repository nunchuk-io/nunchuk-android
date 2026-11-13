package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.separatedevices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import com.nunchuk.android.signer.R as SignerR

/**
 * CLM-KCa / CLM-KCb: before collecting 2 or more keys, recommends one hardware device per key.
 * Exactly 2 keys use the CLM-KCa copy; 3 or more share the CLM-KCb copy.
 */
@Composable
fun ClaimSeparateDevicesScreen(
    keyCount: Int,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    val isTwoKeys = keyCount == 2
    NcScaffold(
        modifier = Modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_add_inheritance_to_claim_first,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinue,
            ) {
                Text(text = stringResource(SignerR.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = stringResource(
                    if (isTwoKeys) R.string.nc_claim_separate_devices_title_two
                    else R.string.nc_claim_separate_devices_title_many
                ),
                style = NunchukTheme.typography.heading,
            )
            Text(
                text = stringResource(R.string.nc_claim_separate_devices_lead),
                style = NunchukTheme.typography.title,
            )
            Text(
                text = stringResource(
                    if (isTwoKeys) R.string.nc_claim_separate_devices_one_device_two
                    else R.string.nc_claim_separate_devices_one_device_many
                ),
                style = NunchukTheme.typography.body,
            )
            Text(
                modifier = Modifier.padding(bottom = 16.dp),
                text = stringResource(
                    if (isTwoKeys) R.string.nc_claim_separate_devices_independent_two
                    else R.string.nc_claim_separate_devices_independent_many
                ),
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimSeparateDevicesTwoKeysPreview() {
    NunchukTheme {
        ClaimSeparateDevicesScreen(keyCount = 2)
    }
}

@PreviewLightDark
@Composable
private fun ClaimSeparateDevicesManyKeysPreview() {
    NunchukTheme {
        ClaimSeparateDevicesScreen(keyCount = 3)
    }
}
