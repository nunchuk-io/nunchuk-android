package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.bufferperiod

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.model.BufferPeriodCountdown
import com.nunchuk.android.core.R
import com.nunchuk.android.main.R as MainR

@Composable
fun ClaimBufferPeriodScreen(
    snackState: SnackbarHostState,
    countdown: BufferPeriodCountdown,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onGotItClick: () -> Unit = {},
) {
    ClaimBufferPeriodContent(
        modifier = modifier,
        snackState = snackState,
        countdown = countdown,
        onBackPressed = onBackPressed,
        onGotItClick = onGotItClick,
    )
}

@Composable
private fun ClaimBufferPeriodContent(
    modifier: Modifier = Modifier,
    snackState: SnackbarHostState,
    countdown: BufferPeriodCountdown,
    onBackPressed: () -> Unit = {},
    onGotItClick: () -> Unit = {},
) {
    NcScaffold(
        modifier = modifier.navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcImageAppBar(
                backgroundRes = MainR.drawable.bg_buffer_period_illustration,
                onClosedClicked = onBackPressed,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_buffer_period_has_started),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_buffer_period_has_started_desc),
                style = NunchukTheme.typography.body
            )
            NcHighlightText(
                modifier = Modifier.padding(16.dp),
                text = stringResource(R.string.nc_check_back_in, countdown.remainingDisplayName),
                style = NunchukTheme.typography.body
            )
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onGotItClick,
            ) {
                Text(text = stringResource(id = R.string.nc_text_got_it))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ClaimBufferPeriodScreenPreview() {
    NunchukTheme {
        ClaimBufferPeriodContent(
            snackState = remember { SnackbarHostState() },
            countdown = BufferPeriodCountdown(0, "0", 0, 0, "0")
        )
    }
}

