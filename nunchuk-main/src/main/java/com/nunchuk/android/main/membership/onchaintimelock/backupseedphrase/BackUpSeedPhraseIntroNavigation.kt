package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NCLabelWithIndex
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object BackUpSeedPhraseIntro

fun NavGraphBuilder.backUpSeedPhraseIntroDestination(
    onContinue: () -> Unit = {}
) {
    composable<BackUpSeedPhraseIntro> {
        BackUpSeedPhraseIntroScreen(onContinue = onContinue)
    }
}

@Composable
private fun BackUpSeedPhraseIntroScreen(
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    BackUpSeedPhraseIntroContent(
        onContinueClicked = onContinue,
        remainTime = remainTime
    )
}

@Composable
private fun BackUpSeedPhraseIntroContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(topBar = {
            NcImageAppBar(
                backgroundRes = R.drawable.bg_seed_phrase_intro_illustration,
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
                    text = "Back up your inheritance key seed phrase",
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Please back up the inheritance key seed phrase on a steel plate, or a durable format that is non-digital.")
                        }
                        append(" You will need to share this seed phrase backup with your Beneficiary so they can access the inheritance.")
                    },
                    style = NunchukTheme.typography.body
                )

                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 1,
                    label = "Turn on your hardware signing device",
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 2,
                    label = "Refer to your device’s instruction manual and find the option to view or back up the recovery words for this key (12 or 24 words)",
                )
                NCLabelWithIndex(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    index = 3,
                    label = "Refer to your device’s instruction manual and find the option to view or back up the recovery words for this key (12 or 24 words)",
                )


                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = "I have backed it up")
                }
            }
        }
    }
}

@Preview
@Composable
private fun BackUpSeedPhraseIntroScreenPreview() {
    BackUpSeedPhraseIntroContent(

    )
}