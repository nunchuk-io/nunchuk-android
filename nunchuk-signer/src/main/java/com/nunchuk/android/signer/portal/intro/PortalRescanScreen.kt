package com.nunchuk.android.signer.portal.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.portal.PortalDeviceArgs
import com.nunchuk.android.core.portal.PortalDeviceFlow
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.signer.R

@Composable
fun PortalRescanScreen(
    args: PortalDeviceArgs,
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
    onScanPortalClicked: () -> Unit = {}
) {
    val signer = args.signer!!
    NcScaffold(
        modifier = modifier.systemBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(title = "")
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = onScanPortalClicked,
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.nc_please_scan_your_portal_again),
                style = NunchukTheme.typography.heading,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NcCircleImage(resId = signer.toReadableDrawableResId())
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .weight(1.0f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = signer.name, style = NunchukTheme.typography.body)
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        text = signer.getXfpOrCardIdLabel(),
                        style = NunchukTheme.typography.bodySmall.copy(
                            color = colorResource(
                                id = R.color.nc_grey_dark_color
                            )
                        ),
                    )
                }
            }

            Text(
                text = stringResource(R.string.nc_please_scan_correct_portal),
                style = NunchukTheme.typography.body,
            )
        }
    }
}

@Preview
@Composable
private fun PortalRescanScreenPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    NunchukTheme {
        PortalRescanScreen(
            args = PortalDeviceArgs(
                signer = signer,
                type = PortalDeviceFlow.RESCAN
            )
        )
    }
}