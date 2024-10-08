package com.nunchuk.android.signer.portal.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.signer.R

@Composable
fun PortalIntroScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
    flow: PortalDeviceFlow = PortalDeviceFlow.SETUP,
    onScanPortalClicked: () -> Unit = {}
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(backgroundRes = R.drawable.nc_bg_portal_intro)
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = onScanPortalClicked
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = if (flow == PortalDeviceFlow.SETUP) stringResource(id = R.string.nc_add_portal)
                else stringResource(id = R.string.nc_portal_tips),
                style = NunchukTheme.typography.heading,
            )

            Text(
                text = stringResource(R.string.nc_add_portal_note_title),
                style = NunchukTheme.typography.body,
            )

            NCLabelWithIndex(
                index = 1,
                title = stringResource(R.string.nc_maintain_nfc_connectivity),
                label = stringResource(R.string.nc_maintain_nfc_connectivity_desc)
            )

            NCLabelWithIndex(
                index = 2,
                title = stringResource(R.string.nc_on_device_confirmation),
                label = stringResource(R.string.nc_on_device_confirmation_desc)
            )
        }
    }
}

@Preview
@Composable
private fun PortalIntroScreenPreview() {
    NunchukTheme {
        PortalIntroScreen()
    }
}