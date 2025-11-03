package com.nunchuk.android.signer.portal.setup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.software.components.recover.RecoverSeedActivity

const val selectSetupSeedPhraseRoute = "select_setup_seed_phrase"

fun NavGraphBuilder.selectSetupSeedPhrase(
    openSetPassphraseScreen: (String, Int) -> Unit = { _, _ -> },
    openSelectNumberOfWords: (SeedPhraseType) -> Unit = {},
) {
    composable(selectSetupSeedPhraseRoute) {
        SelectSetupSeedPhraseScreen(
            openSetPassphraseScreen = openSetPassphraseScreen,
            openSelectNumberOfWords = openSelectNumberOfWords
        )
    }
}

fun NavController.navigateSelectSetupSeedPhrase(
    navOptions: NavOptions? = null,
) {
    navigate(selectSetupSeedPhraseRoute, navOptions)
}

enum class SeedPhraseType {
    ON_PORTAL,
    NUNCHUK,
    IMPORT
}

@Composable
fun SelectSetupSeedPhraseScreen(
    modifier: Modifier = Modifier,
    openSelectNumberOfWords: (SeedPhraseType) -> Unit = {},
    openSetPassphraseScreen: (String, Int) -> Unit = { _, _ -> },
) {
    var selectedType by remember { mutableStateOf(SeedPhraseType.ON_PORTAL) }
    val context = LocalContext.current

    val importSeedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val mnemonic = it.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            if (mnemonic.isNotEmpty()) {
                openSetPassphraseScreen(mnemonic, 0)
            }
        }
    }

    NcScaffold(
        modifier = modifier.systemBarsPadding(),
        topBar = {
            NcTopAppBar(title = "")
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (selectedType == SeedPhraseType.IMPORT) {
                        importSeedLauncher.launch(
                            RecoverSeedActivity.buildIntent(
                                keyFlow = KeyFlow.ADD_AND_RETURN,
                                activityContext = context,
                            )
                        )
                    } else openSelectNumberOfWords(selectedType)
                }) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues = paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.nc_select_a_seed_phrase),
                style = NunchukTheme.typography.heading
            )

            Text(
                text = stringResource(R.string.nc_select_a_seed_phrase_desc),
                style = NunchukTheme.typography.body
            )

            NcRadioOption(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                isSelected = selectedType == SeedPhraseType.ON_PORTAL,
                onClick = {
                    selectedType = SeedPhraseType.ON_PORTAL
                }
            ) {
                Text(
                    text = stringResource(R.string.nc_generate_seed_on_portal),
                    style = NunchukTheme.typography.title
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.nc_generate_seed_on_portal_desc),
                    style = NunchukTheme.typography.body
                )
            }
            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = selectedType == SeedPhraseType.NUNCHUK,
                onClick = {
                    selectedType = SeedPhraseType.NUNCHUK
                }
            ) {
                Text(
                    text = stringResource(R.string.nc_generate_seed_in_nunchuk),
                    style = NunchukTheme.typography.title
                )

                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.nc_generate_seed_in_nunchuk_desc),
                    style = NunchukTheme.typography.body
                )
            }
            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                isSelected = selectedType == SeedPhraseType.IMPORT,
                onClick = {
                    selectedType = SeedPhraseType.IMPORT
                }
            ) {
                Text(
                    text = stringResource(R.string.nc_import_existing_seed_phrase_to_portal),
                    style = NunchukTheme.typography.title
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectSetupSeedPhraseScreenPreview() {
    NunchukTheme {
        SelectSetupSeedPhraseScreen()
    }
}