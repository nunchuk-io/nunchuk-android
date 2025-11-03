package com.nunchuk.android.signer.portal.seed

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.signer.KeyFlow
import com.nunchuk.android.share.result.GlobalResultKey
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.portal.setup.SeedPhraseType
import com.nunchuk.android.signer.software.components.create.CreateNewSeedActivity

const val selectNumberWordRoute = "select_number_word/{type}"

fun NavGraphBuilder.selectNumberWord(
    openSetPassphraseScreen: (String, Int) -> Unit = { _, _ -> },
) {
    // add new compose navigation with type as parameter
    composable(selectNumberWordRoute, arguments = listOf(
        navArgument("type") {
            type = NavType.StringType
            nullable = false
        }
    )) {
        val type = it.arguments?.getString("type").orEmpty()
        val seedPhraseType = SeedPhraseType.valueOf(type)
        SelectNumberWordScreen(
            seedPhraseType = seedPhraseType,
            openSetPassphraseScreen = openSetPassphraseScreen,
        )
    }
}

fun NavController.navigateToSelectNumberWord(seedPhraseType: SeedPhraseType) {
    navigate("select_number_word/${seedPhraseType.name}")
}

@Composable
fun SelectNumberWordScreen(
    modifier: Modifier = Modifier,
    seedPhraseType: SeedPhraseType,
    openSetPassphraseScreen: (String, Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val generateSeedLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val mnemonic = it.data?.getStringExtra(GlobalResultKey.MNEMONIC).orEmpty()
            if (mnemonic.isNotEmpty()) {
                openSetPassphraseScreen(mnemonic, 0)
            }
        }
    }
    var numberOfWords by rememberSaveable { mutableIntStateOf(12) }
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
                    when (seedPhraseType) {
                        SeedPhraseType.ON_PORTAL -> {
                            openSetPassphraseScreen("", numberOfWords)
                        }

                        SeedPhraseType.NUNCHUK -> {
                            generateSeedLauncher.launch(
                                CreateNewSeedActivity.buildIntent(
                                    activityContext = context,
                                    keyFlow = KeyFlow.ADD_AND_RETURN,
                                    numberOfWords = numberOfWords,
                                )
                            )
                        }

                        else -> Unit
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "How many seed words do you want?",
                style = NunchukTheme.typography.heading
            )

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                onClick = { numberOfWords = 12 },
                isSelected = numberOfWords == 12,
            ) {
                Text(
                    text = "12 words (recommended)",
                    style = NunchukTheme.typography.title
                )
            }

            NcRadioOption(
                modifier = Modifier.fillMaxWidth(),
                onClick = { numberOfWords = 24 },
                isSelected = numberOfWords == 24,
            ) {
                Text(
                    text = "24 words",
                    style = NunchukTheme.typography.title
                )
            }
        }
    }
}

@Preview
@Composable
private fun SelectNumberWordScreenPreview() {
    NunchukTheme {
        SelectNumberWordScreen(seedPhraseType = SeedPhraseType.ON_PORTAL)
    }
}