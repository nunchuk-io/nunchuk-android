package com.nunchuk.android.main.membership.onchaintimelock.backupseedphrase

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object BackUpSeedPhraseVerifySuccess

fun NavGraphBuilder.backUpSeedPhraseVerifySuccessDestination(
    onContinue: () -> Unit = {},
    onMoreClicked: () -> Unit = {}
) {
    composable<BackUpSeedPhraseVerifySuccess> {
        BackUpSeedPhraseVerifySuccessScreen(
            onContinue = onContinue,
            onMoreClicked = onMoreClicked
        )
    }
}

@Composable
private fun BackUpSeedPhraseVerifySuccessScreen(
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    BackUpSeedPhraseVerifySuccessContent(
        onContinueClicked = onContinue,
        remainTime = remainTime,
        onMoreClicked = onMoreClicked
    )
}

@Composable
private fun BackUpSeedPhraseVerifySuccessContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
) {
    var verifyNow by remember { mutableStateOf(true) }
    NunchukTheme {
        Scaffold(topBar = {
            NcTopAppBar(
                title = if (remainTime <= 0) "" else stringResource(
                    id = R.string.nc_estimate_remain_time,
                    remainTime
                ),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                }
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nc_green_stick),
                        contentDescription = ""
                    )
                }

                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = "You have backed up the seed phrase correctly",
                    style = NunchukTheme.typography.heading
                )

                Text(
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    text = "The restored key’s public key matches the public key of your inheritance key.",
                    style = NunchukTheme.typography.body
                )

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
private fun BackUpSeedPhraseVerifySuccessScreenPreview() {
    BackUpSeedPhraseVerifySuccessContent(

    )
}