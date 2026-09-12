package com.nunchuk.android.main.membership.backupseedphrase

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.SignerModelProvider
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import kotlinx.serialization.Serializable

@Serializable
object BackUpSeedPhraseVerifyMismatch

fun NavGraphBuilder.backUpSeedPhraseVerifyMismatchDestination(
    reAddedSigner: SignerModel? = null,
    expectedXfp: String = "",
    onTryAgain: () -> Unit = {},
    onBackToSteps: () -> Unit = {},
) {
    composable<BackUpSeedPhraseVerifyMismatch> {
        BackUpSeedPhraseVerifyMismatchScreen(
            reAddedSigner = reAddedSigner,
            expectedXfp = expectedXfp,
            onTryAgain = onTryAgain,
            onBackToSteps = onBackToSteps,
        )
    }
}

@Composable
private fun BackUpSeedPhraseVerifyMismatchScreen(
    reAddedSigner: SignerModel? = null,
    expectedXfp: String = "",
    viewModel: BackUpSeedPhraseSharedViewModel = hiltViewModel(),
    onTryAgain: () -> Unit = {},
    onBackToSteps: () -> Unit = {},
) {
    val remainTime by viewModel.remainTime.collectAsStateWithLifecycle()
    BackUpSeedPhraseVerifyMismatchContent(
        reAddedSigner = reAddedSigner,
        expectedXfp = expectedXfp,
        remainTime = remainTime,
        onTryAgain = onTryAgain,
        onBackToSteps = onBackToSteps,
    )
}

/**
 * The restored device derived a different public key, so this seed phrase would not recover the
 * inheritance key — which is the whole point of verifying. Nothing is deleted here: the
 * inheritance key is untouched and only the restored copy failed to match, so the way out is to
 * re-enter the words and try again.
 */
@Composable
private fun BackUpSeedPhraseVerifyMismatchContent(
    reAddedSigner: SignerModel? = null,
    expectedXfp: String = "",
    remainTime: Int = 0,
    onTryAgain: () -> Unit = {},
    onBackToSteps: () -> Unit = {},
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = if (remainTime <= 0) "" else stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                )
            },
            bottomBar = {
                Column(modifier = Modifier.padding(16.dp)) {
                    NcPrimaryDarkButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onTryAgain,
                    ) {
                        Text(text = stringResource(R.string.nc_try_again))
                    }
                    NcOutlineButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        onClick = onBackToSteps,
                    ) {
                        Text(text = stringResource(R.string.nc_back_to_seed_phrase_steps))
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    modifier = Modifier.padding(top = 72.dp),
                    painter = painterResource(id = R.drawable.nc_badge_not_matched),
                    contentDescription = null,
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp),
                    text = stringResource(R.string.nc_key_does_not_match),
                    style = NunchukTheme.typography.heading,
                    textAlign = TextAlign.Center,
                )
                Text(
                    modifier = Modifier.padding(top = 16.dp),
                    text = stringResource(R.string.nc_key_does_not_match_desc),
                    style = NunchukTheme.typography.body,
                    textAlign = TextAlign.Center,
                )
                if (reAddedSigner != null) {
                    VerificationSignerCard(
                        modifier = Modifier.padding(top = 24.dp),
                        signer = reAddedSigner.copy(
                            name = stringResource(
                                R.string.nc_signer_name_re_added,
                                reAddedSigner.name
                            )
                        ),
                        statusText = stringResource(
                            R.string.nc_xfp_expected,
                            reAddedSigner.fingerPrint.uppercase(),
                            expectedXfp.uppercase(),
                        ),
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun BackUpSeedPhraseVerifyMismatchContentPreview(
    @PreviewParameter(SignerModelProvider::class) signer: SignerModel,
) {
    BackUpSeedPhraseVerifyMismatchContent(
        reAddedSigner = signer,
        expectedXfp = "79eb35f4",
    )
}
