package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

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
fun PrepareInheritanceKeyScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onContinue: (InheritanceOption) -> Unit = {},
) {
    var selectedOption by remember { mutableStateOf(InheritanceOption.HARDWARE_DEVICE) }

    Scaffold(
        modifier = modifier.navigationBarsPadding(),
        topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.nc_bg_prepare_inheritance_key,
                onClosedClicked = onBackPressed,
            )
        },
        bottomBar = {
            Column(Modifier.padding(16.dp)) {
                NcPrimaryDarkButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onContinue(selectedOption) }
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
                text = stringResource(R.string.nc_prepare_inheritance_key),
                style = NunchukTheme.typography.heading,
                modifier = Modifier.padding(top = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.nc_prepare_inheritance_key_desc),
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.nc_inheritance_key_how_proceed),
                style = NunchukTheme.typography.body
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedOption == InheritanceOption.HARDWARE_DEVICE,
                    onClick = { selectedOption = InheritanceOption.HARDWARE_DEVICE }
                ) {
                    Text(
                        text = stringResource(R.string.nc_i_have_hardware_device),
                        style = NunchukTheme.typography.title
                    )
                }
                
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = selectedOption == InheritanceOption.SEED_PHRASE,
                    onClick = { selectedOption = InheritanceOption.SEED_PHRASE }
                ) {
                    Text(
                        text = stringResource(R.string.nc_i_have_seed_phrase_backup),
                        style = NunchukTheme.typography.title
                    )
                }
            }
        }
    }
}

enum class InheritanceOption {
    HARDWARE_DEVICE,
    SEED_PHRASE
}

@PreviewLightDark
@Composable
private fun PrepareInheritanceKeyScreenPreview() {
    NunchukTheme {
        PrepareInheritanceKeyScreen()
    }
}
