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
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.signer.R

const val portalIntroRoute = "portal_intro_route"

fun NavGraphBuilder.portalIntro(
    snackState: SnackbarHostState = SnackbarHostState(),
    onScanPortalClicked: () -> Unit = {},
) {
    composable(portalIntroRoute) {
        PortalIntroScreen(
            onScanPortalClicked = onScanPortalClicked,
            snackState = snackState
        )
    }
}

@Composable
fun PortalIntroScreen(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState = SnackbarHostState(),
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
                text = stringResource(id = R.string.nc_add_portal),
                style = NunchukTheme.typography.heading,
            )

            Text(
                text = "Please take note of the following before you begin:",
                style = NunchukTheme.typography.body,
            )

            NCLabelWithIndex(
                index = 1,
                title = "Maintain NFC connectivity",
                label = "To operate, Portal requires continuous power from the mobile device via NFC. Please place the Portal device on a flat surface, then put the mobile device on top of it, aligning the NFC chips."
            )

            NCLabelWithIndex(
                index = 2,
                title = "On-device confirmation",
                label = "To operate, Portal requires continuous power from the mobile device via NFC. Please place the Portal device on a flat surface, then put the mobile device on top of it, aligning the NFC chips."
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
