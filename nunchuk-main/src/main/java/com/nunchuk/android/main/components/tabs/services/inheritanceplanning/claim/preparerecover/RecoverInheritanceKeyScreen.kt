package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.preparerecover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.main.R

@Composable
fun RecoverInheritanceKeyScreen(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit = {},
    onContinue: (Boolean) -> Unit = {},
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
                    onClick = { onContinue(selectedOption == RecoveryOption.HARDWARE_DEVICE) }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.nc_use_hardware_device),
                            style = NunchukTheme.typography.title
                        )
                        Text(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.backgroundMidGray,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            text = stringResource(R.string.nc_recommended),
                            style = NunchukTheme.typography.bodySmall
                        )
                    }
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
