package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object BackUpSeedPhraseOption

fun NavGraphBuilder.backUpSeedPhraseOptionDestination(
    onContinue: () -> Unit = {}
) {
    composable<BackUpSeedPhraseOption> {
        BackUpSeedPhraseOptionScreen(onContinue = onContinue)
    }
}

@Composable
private fun BackUpSeedPhraseOptionScreen(
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    BackUpSeedPhraseOptionContent(
        onContinueClicked = onContinue,
        remainTime = remainTime
    )
}

@Composable
private fun BackUpSeedPhraseOptionContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    var verifyNow by remember { mutableStateOf(true) }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                )
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = "Verify your inheritance key seed phrase",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = "We recommend verifying now to confirm that your seed phrase backup works.",
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                    text = "This ensures it is correct and can be used to recover the inheritance key in the future.",
                    style = NunchukTheme.typography.body
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 16.dp),
                    text = "Would you like to verify the backup now?",
                    style = NunchukTheme.typography.body
                )
                
                NcRadioOption(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    isSelected = verifyNow,
                    onClick = { verifyNow = true }
                ) {
                    Row {
                        Text(
                            text = "Verify now",
                            style = NunchukTheme.typography.title
                        )
                        NcTag(
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                            label = "Recommended"
                        )
                    }
                }
                
                NcRadioOption(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    isSelected = !verifyNow,
                    onClick = { verifyNow = false }
                ) {
                    Text(
                        text = "Skip verification",
                        style = NunchukTheme.typography.title
                    )
                }


                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = com.nunchuk.android.signer.R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackUpSeedPhraseOptionScreenPreview() {
    BackUpSeedPhraseOptionContent(

    )
}