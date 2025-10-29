package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.restorehardware

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcLabelWithIndex
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.R
import com.nunchuk.android.main.R as MainR

@Composable
fun RestoreSeedPhraseHardwareScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = MainR.drawable.nc_bg_seed_phrase_hardware_device,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinue
                ) {
                    Text(text = stringResource(MainR.string.nc_text_continue))
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(MainR.string.nc_restore_seed_phrase_hardware_device),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NcLabelWithIndex(
                    index = 1,
                    label = stringResource(MainR.string.nc_restore_hardware_step_1)
                )
                NcLabelWithIndex(
                    index = 2,
                    label = stringResource(MainR.string.nc_restore_hardware_step_2)
                )
                NcLabelWithIndex(
                    index = 3,
                    label = stringResource(MainR.string.nc_restore_hardware_step_3)
                )
                NcLabelWithIndex(
                    index = 4,
                    label = stringResource(MainR.string.nc_restore_hardware_step_4)
                )
                NcLabelWithIndex(
                    index = 5,
                    label = stringResource(MainR.string.nc_restore_hardware_step_5)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.greyLight,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(MainR.string.nc_restore_seed_phrase_warning),
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun RestoreSeedPhraseHardwareScreenPreview() {
    NunchukTheme {
        RestoreSeedPhraseHardwareScreen()
    }
}

