package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

@Composable
fun RecoverInheritanceKeyScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onContinue: () -> Unit = {},
) {
    var selectedOption by remember { mutableStateOf(RecoveryOption.HARDWARE_DEVICE) }

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_inheritance_recover_seed_phrase,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onContinue
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
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
                text = stringResource(R.string.nc_recover_inheritance_key),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            Text(
                text = stringResource(R.string.nc_recover_inheritance_key_desc_hardware),
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(top = 16.dp)
            )

            Text(
                text = stringResource(R.string.nc_recover_inheritance_key_desc_direct),
                style = NunchukTheme.typography.body,
                modifier = Modifier.padding(top = 16.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedOption == RecoveryOption.HARDWARE_DEVICE,
                    onClick = { selectedOption = RecoveryOption.HARDWARE_DEVICE }
                ) {
                    Text(
                        text = stringResource(R.string.nc_use_hardware_device),
                        style = NunchukTheme.typography.title
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.nc_recommended),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedOption == RecoveryOption.ENTER_DIRECTLY,
                    onClick = { selectedOption = RecoveryOption.ENTER_DIRECTLY }
                ) {
                    Text(
                        text = stringResource(R.string.nc_enter_seed_phrase_directly),
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

private enum class RecoveryOption {
    HARDWARE_DEVICE,
    ENTER_DIRECTLY
}

@PreviewLightDark
@Composable
private fun RecoverInheritanceKeyScreenPreview() {
    NunchukTheme {
        RecoverInheritanceKeyScreen()
    }
}
