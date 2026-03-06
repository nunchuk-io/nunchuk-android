package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.releaseintro

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

@Composable
internal fun ClaimReleaseIntroScreen(
    snackState: SnackbarHostState,
    onBackPressed: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    ClaimReleaseIntroContent(
        snackState = snackState,
        onBackPressed = onBackPressed,
        onContinueClicked = onContinueClicked,
    )
}

@Composable
private fun ClaimReleaseIntroContent(
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onBackPressed: () -> Unit = {},
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            snackState = snackState,
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_inheritance_release_intro,
                    onClosedClicked = onBackPressed,
                )
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.nc_how_your_inheritance_is_released),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_release_intro_desc_1),
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_release_intro_desc_2),
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(id = R.string.nc_release_intro_desc_3),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimReleaseIntroScreenPreview() {
    NunchukTheme {
        ClaimReleaseIntroContent()
    }
}
