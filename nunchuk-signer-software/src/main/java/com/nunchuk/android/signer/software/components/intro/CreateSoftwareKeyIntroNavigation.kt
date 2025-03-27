package com.nunchuk.android.signer.software.components.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcOptionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheet
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlTextPrimary
import com.nunchuk.android.signer.software.R

const val createSoftwareKeyIntroRoute = "create_software_key_intro"

fun NavGraphBuilder.createSoftwareKeyIntro(
    isSupportXprv: Boolean,
    onContinueClicked: (Boolean) -> Unit = {},
    onRecoverSeedClicked: () -> Unit = {},
    onRecoverXprvClicked: () -> Unit = {},
) {
    composable(createSoftwareKeyIntroRoute) {
        CreateSoftwareKeyIntroScreen(
            isSupportXprv = isSupportXprv,
            onContinueClicked = onContinueClicked,
            onRecoverSeedClicked = onRecoverSeedClicked,
            onRecoverXprvClicked = onRecoverXprvClicked,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSoftwareKeyIntroScreen(
    isSupportXprv: Boolean = false,
    onContinueClicked: (Boolean) -> Unit = {},
    onRecoverSeedClicked: () -> Unit = {},
    onRecoverXprvClicked: () -> Unit = {},
) {
    var showRecoverSheet by rememberSaveable { mutableStateOf(false) }
    var selectedOption by remember { mutableIntStateOf(CreateOption.None.ordinal) }

    NunchukTheme {
        NcScaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.nc_software_key_illustration,
                    title = "",
                )
            }, bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .fillMaxWidth(),
                        enabled = selectedOption != CreateOption.None.ordinal,
                        onClick = {
                            if (selectedOption == CreateOption.RecoverSeed.ordinal) {
                                if (isSupportXprv) {
                                    showRecoverSheet = true
                                } else {
                                    onRecoverSeedClicked()
                                }
                            } else {
                                onContinueClicked(selectedOption == CreateOption.CreateAndBackup.ordinal)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.nc_text_continue),
                            style = NunchukTheme.typography.title.copy(color = MaterialTheme.colorScheme.controlTextPrimary)
                        )
                    }
                }

            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "Create software key",
                    style = NunchukTheme.typography.heading,
                    modifier = Modifier.padding(top = 12.dp)
                )

                Text(
                    text = "A software key will be generated locally on this device. To ensure security, keep this device safe to prevent unauthorized access. Deleting the app will also delete the software key.",
                    style = NunchukTheme.typography.body,
                )

                NcOptionItem(
                    modifier = Modifier.padding(top = 8.dp),
                    isSelected = selectedOption == CreateOption.CreateAndBackup.ordinal,
                    label = "Create and back up key now",
                    onClick = {
                        selectedOption = CreateOption.CreateAndBackup.ordinal
                    }
                )

                NcOptionItem(
                    isSelected = selectedOption == CreateOption.CreateAndBackupLater.ordinal,
                    label = "Create key now, back up later",
                    onClick = {
                        selectedOption = CreateOption.CreateAndBackupLater.ordinal
                    }
                )

                NcOptionItem(
                    isSelected = selectedOption == CreateOption.RecoverSeed.ordinal,
                    label = "Recover existing seed",
                    onClick = {
                        selectedOption = CreateOption.RecoverSeed.ordinal
                    }
                )
            }
        }
    }

    if (showRecoverSheet) {
        NcSelectableBottomSheet(
            options = listOf(
                stringResource(R.string.nc_recover_key_via_seed),
                stringResource(R.string.nc_recover_key_via_xprv),
            ),
            onSelected = {
                if (it == 0) {
                    onRecoverSeedClicked()
                } else {
                    onRecoverXprvClicked()
                }
                showRecoverSheet = false
            },
            onDismiss = {
                showRecoverSheet = false
            },
        )
    }
}

enum class CreateOption {
    None,
    CreateAndBackup,
    CreateAndBackupLater,
    RecoverSeed,
}

@Preview
@Composable
private fun SoftwareSignerIntroPreview() {
    NunchukTheme {
        CreateSoftwareKeyIntroScreen()
    }
}